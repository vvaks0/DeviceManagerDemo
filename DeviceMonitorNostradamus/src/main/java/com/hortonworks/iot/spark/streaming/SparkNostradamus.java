package com.hortonworks.iot.spark.streaming;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;

import com.google.common.base.Optional;
import com.hortonworks.iot.events.STBStatus;
import com.hortonworks.iot.util.Constants;

import scala.Tuple2;

public class SparkNostradamus {
	
	public static void main(String[] args) {
		final Integer batchSize = 1;
		final String pubSubUrl = Constants.pubSubUrl;
		final String predictionChannel = Constants.predictionChannel;
		final String tempFailPredication = "Temperature pattern indicates imminent device failure. Contact customer or send technician";
		
		Map<String, Integer> kafkaTopics = new HashMap<String, Integer>();
		kafkaTopics.put(Constants.deviceTopicName, 1);
		SparkConf sparkConf = new SparkConf();//.setMaster("local[4]").setAppName("Nostradamus").set("spark.driver.allowMultipleContexts", "true");
		
		JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(batchSize));
		jssc.checkpoint(Constants.sparkCheckpointPath);
		final SVMModel nostradamus = SVMModel.load(jssc.sparkContext().sc(), Constants.sparkModelPath+"nostradamusSVMModel");
		
		JavaPairReceiverInputDStream<String, String> kafkaStream = 
		KafkaUtils.createStream(jssc, Constants.zkConnString,"spark-streaming-consumer-group", kafkaTopics);
				
		//kafkaStream.print();
		JavaPairDStream<String, String> deviceStream = kafkaStream;
		deviceStream.print();
		JavaPairDStream<String, String> deviceStream2 = deviceStream.mapToPair(new PairFunction<Tuple2<String, String>, String, String> (){
			public Tuple2<String, String> call(Tuple2<String, String> rdd) throws Exception {
				ObjectMapper mapper = new ObjectMapper();
			    STBStatus stbStatus = mapper.readValue(rdd._2, STBStatus.class);
			    //return new Tuple2(stbStatus.getSerialNumber(), rdd._2);
			    return new Tuple2(stbStatus.getSerialNumber(), stbStatus.getInternalTemp().toString());
			}
		});
		
		deviceStream2.print();
		
		JavaPairDStream<String, String> deviceStream3 = deviceStream2.updateStateByKey(new Function2<List<String>, Optional<String>, Optional<String>>() {
			public Optional<String> call(List<String> eventList, Optional<String> tempEventWindow) throws Exception {
				String[] eventWindowArray = null;
				
				//Check if State is Present
				if(tempEventWindow.isPresent()){
					//Turn state from String to Array
					System.out.println("Temp Event Window: " + tempEventWindow.get().replaceAll("Optional.of\\(", "").replaceAll("\\)", ""));
					System.out.println("Temp Event Window Split Length: " + tempEventWindow.get().split(",").length);
					System.out.println("EventList Size: " + eventList.size());
					eventWindowArray = new String[eventList.size() + tempEventWindow.get().split(",").length];
					eventWindowArray = tempEventWindow.get().replaceAll("Optional.of\\(", "").replaceAll("\\)", "").split(",");
					System.out.println("eventWindowArray Length: " + eventWindowArray.length);
					
					List<String> featureWindowList = new ArrayList<String>(Arrays.asList(eventWindowArray));
					System.out.println("featureWindowList Length: " + featureWindowList.size());
					
					//If state list size == 10 (expected feature Vector size), clear list and add new events
					if(featureWindowList.size() == 10){
						featureWindowList.clear();
						for(int i=0; i<eventList.size(); i++){
							featureWindowList.add(eventList.get(i));	
						}
					//If state list size > 10 (expected feature Vector size), remove elements in position <= 10 
					}else if(featureWindowList.size() > 10){
						featureWindowList.subList(0, 9).clear();
					//Add new events to state list
						for(int i=0; i<eventList.size(); i++){
							featureWindowList.add(eventList.get(i));	
						}
					//If state < 10, add new events to eventWindow array	
					}else{
						System.out.println("eventWindowArray Length: " + eventWindowArray.length);
						for(int i=0; i<eventList.size(); i++){
							featureWindowList.add(eventList.get(i));	
						}
					}
					System.out.println("featureWindowList: " + featureWindowList.toString());
					//Convert state list back to Optional String
					for(int k=0; k<featureWindowList.size(); k++){
						if(k==0){
							tempEventWindow = Optional.of(featureWindowList.get(k));
						}else{
							tempEventWindow = Optional.of(tempEventWindow + "," + featureWindowList.get(k));
						}
					}
				}else{
				//If state Optional does not exist, create it	
					for(int i=0; i<eventList.size(); i++){
						if(i==0){
							tempEventWindow = Optional.of(eventList.get(i));
						}else{
							tempEventWindow = Optional.of(tempEventWindow + "," + eventList.get(i));
						}
					}
				}
				
				return tempEventWindow;
			}
		});
		
		deviceStream3.foreachRDD(new Function<JavaPairRDD<String, String>, Void>() { 
			public Void call(JavaPairRDD<String, String> rdd) throws Exception {
				List<Tuple2<String, String>> featuresList = rdd.take(10);
				String[] featuresStringArray = null;
				System.out.println("ForEach Features List: " + featuresList.toString());
				String deviceSerialNumber = "";
				
				if(featuresList.size() > 0){
					for(Tuple2<String,String> deviceFeatures: featuresList){
						deviceSerialNumber = deviceFeatures._1;
						featuresStringArray = deviceFeatures._2.replaceAll("Optional.of\\(", "").replaceAll("\\)", "").split(",");
					
						if(featuresStringArray!=null && featuresStringArray.length == 10){
							double[] featuresArray = new double[featuresStringArray.length];
							for(int i = 0; i < featuresStringArray.length; i++) featuresArray[i] = Double.parseDouble(featuresStringArray[i]);
						
							Vector predictionFeatures = Vectors.dense(featuresArray);	
							Double prediction = nostradamus.predict(predictionFeatures);
							if(prediction == 1.0){
								System.out.println("*********************************************************************************");
								System.out.println("**********************DEVICE FAILURE IMMINENT: " + prediction + predictionFeatures);
								System.out.println("*********************************************************************************");
								Map<String, String> data = new HashMap<String, String>();
								data.put("deviceSerialNumber", deviceSerialNumber);
								data.put("predictionDescription", tempFailPredication);
								
								BayeuxClient bayuexClient = connectPubSub(pubSubUrl);
								bayuexClient.getChannel(predictionChannel).publish(data);
							}else{
								System.out.println("*********************************************************************************");
								System.out.println("**********************DEVICE FUNCTION NORMAL : " + prediction + predictionFeatures);
								System.out.println("*********************************************************************************");
							}
						}else
							System.out.println("*********************************************************************************");
							System.out.println("*********Feature Vector size has not reached required length to make a prediction");
							System.out.println("*********************************************************************************");
					}	
				}else
					featuresStringArray = new String[0];
				
				return null;
			}
        });
				
		/*
		JavaDStream<Tuple2<String, Double>> DeviceStream2000 = kafkaStream.map(new Function<Tuple2<String, String>, Tuple2<String, Double>>() {
			public Tuple2<String, Double> call(Tuple2<String, String> rdd) throws JsonParseException, JsonMappingException, IOException {
				ObjectMapper mapper = new ObjectMapper();
			    STBStatus stbStatus = mapper.readValue(rdd._2, STBStatus.class);
			    return new Tuple2(stbStatus.getSerialNumber(), Double.valueOf(stbStatus.getInternalTemp().toString()));
		    }
		}).filter(new Function<Tuple2<String, Double>, Boolean>() {
			        public Boolean call(Tuple2<String, Double> stbStatus) {
			            return stbStatus._1.equalsIgnoreCase("2000");
			        }
			    }
			);
		
		JavaDStream<Tuple2<String, Double>> DeviceStream3000 = kafkaStream.map(new Function<Tuple2<String, String>, Tuple2<String, Double>>() {
			public Tuple2<String, Double> call(Tuple2<String, String> rdd) throws JsonParseException, JsonMappingException, IOException {
				ObjectMapper mapper = new ObjectMapper();
			    STBStatus stbStatus = mapper.readValue(rdd._2, STBStatus.class);
			    return new Tuple2(stbStatus.getSerialNumber(), Double.valueOf(stbStatus.getInternalTemp().toString()));
		    }
		}).filter(new Function<Tuple2<String, Double>, Boolean>() {
			        public Boolean call(Tuple2<String, Double> stbStatus) {
			            return stbStatus._1.equalsIgnoreCase("3000");
			        }
			    }
			);*/
		
		/*
		DeviceStream1000.foreachRDD(new Function<JavaRDD<Tuple2<String, Double>>, Void>() { 
			public Void call(JavaRDD<Tuple2<String, Double>> rdd) throws Exception {
				List<Tuple2<String, Double>> featuresList = rdd.take(batchSize);
				String deviceSerialNumber = featuresList.get(1)._1;
				double[] featuresArray = new double[featuresList.size()];
				for(int i = 0; i < featuresList.size(); i++) featuresArray[i] = featuresList.get(i)._2;
				
				Vector predictionFeatures = Vectors.dense(featuresArray);	
				Double prediction = nostradamus.predict(predictionFeatures);
				if(prediction == 1.0){
					System.out.println("**********************DEVICE FAILURE IMMINENT: " + prediction + predictionFeatures);
					Map<String, String> data = new HashMap<String, String>();
					data.put("deviceSerialNumber", deviceSerialNumber);
					data.put("predictionDescription", tempFailPredication);
					bayuexClient.getChannel(predictionChannel).publish(data);
				}else{
					System.out.println("**********************DEVICE Function Normal : " + prediction + predictionFeatures);
				}
				
				return null;
			}
        }); */
		/*
		DeviceStream2000.foreachRDD(new Function<JavaRDD<Tuple2<String, Double>>, Void>() { 
			public Void call(JavaRDD<Tuple2<String, Double>> rdd) throws Exception {
				List<Tuple2<String, Double>> featuresList = rdd.take(batchSize);
				String deviceSerialNumber = featuresList.get(1)._1;
				double[] featuresArray = new double[featuresList.size()];
				for(int i = 0; i < featuresList.size(); i++) featuresArray[i] = featuresList.get(i)._2;
				
				Vector predictionFeatures = Vectors.dense(featuresArray);	
				Double prediction = nostradamus.predict(predictionFeatures);
				if(prediction == 1.0){
					System.out.println("**********************DEVICE FAILURE IMMINENT: " + prediction + predictionFeatures);
				}else{
					System.out.println("**********************DEVICE Function Normal : " + prediction + predictionFeatures);
					Map<String, String> data = new HashMap<String, String>();
					data.put("deviceSerialNumber", deviceSerialNumber);
					data.put("predictionDescription", tempFailPredication);
					bayuexClient.getChannel(predictionChannel).publish(data);
				}
				
				return null;
			}
        });
		
		DeviceStream3000.foreachRDD(new Function<JavaRDD<Tuple2<String, Double>>, Void>() { 
			public Void call(JavaRDD<Tuple2<String, Double>> rdd) throws Exception {
				List<Tuple2<String, Double>> featuresList = rdd.take(batchSize);
				String deviceSerialNumber = featuresList.get(1)._1;
				double[] featuresArray = new double[featuresList.size()];
				for(int i = 0; i < featuresList.size(); i++) featuresArray[i] = featuresList.get(i)._2;
				
				Vector predictionFeatures = Vectors.dense(featuresArray);	
				Double prediction = nostradamus.predict(predictionFeatures);
				if(prediction == 1.0){
					System.out.println("**********************DEVICE FAILURE IMMINENT: " + prediction + predictionFeatures);
					Map<String, String> data = new HashMap<String, String>();
					data.put("deviceSerialNumber", deviceSerialNumber);
					data.put("predictionDescription", tempFailPredication);
					bayuexClient.getChannel(predictionChannel).publish(data);
				}else{
					System.out.println("**********************DEVICE Function Normal : " + prediction + predictionFeatures);
				}
				
				return null;
			}
        }); */
		
		jssc.start();
		jssc.awaitTermination();
	}
	
	public static BayeuxClient connectPubSub(String pubSubUrl) {
		HttpClient httpClient = new HttpClient();
		BayeuxClient bayuexClient;
		
		try {
			httpClient.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Prepare the transport
		Map<String, Object> options = new HashMap<String, Object>();
		ClientTransport transport = new LongPollingTransport(options, httpClient);

		// Create the BayeuxClient
		bayuexClient = new BayeuxClient(pubSubUrl, transport);
		
		bayuexClient.handshake();
		boolean handshaken = bayuexClient.waitFor(3000, BayeuxClient.State.CONNECTED);
		if (handshaken)
		{
			System.out.println("Connected to Cometd Http PubSub Platform");
		}
		else{
			System.out.println("Could not connect to Cometd Http PubSub Platform");
		}
		return bayuexClient;
	}
}