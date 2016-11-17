package com.hortonworks.iot.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka._
import org.apache.spark.mllib.classification.SVMModel
import org.apache.spark.mllib.linalg.Vectors
import org.cometd.client.BayeuxClient
import org.cometd.client.transport.{ClientTransport,LongPollingTransport}
import org.eclipse.jetty.client.HttpClient
import scala.util.parsing.json._

object SparkNostradamus {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("SparkNostradamus")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    ssc.sparkContext.setLogLevel("WARN")
    ssc.checkpoint("/demo/data/checkpoint");
    val nostradamus = SVMModel.load(ssc.sparkContext, "/demo/data/model/nostradamusSVMModel");
    val kafkaConsumerGroup = "spark-streaming-group"
    val zkQuorum = args(0)
    val deviceEventsTopic = args(1)
    val cometdUrl = ssc.sparkContext.broadcast("http://" + args(2) + "/cometd")
    val predictionChannel = ssc.sparkContext.broadcast("/prediction")
    val kafkaTopicConfig = Map(deviceEventsTopic -> 1)    
    val deviceStreamJSON = KafkaUtils.createStream(ssc, zkQuorum, kafkaConsumerGroup, kafkaTopicConfig)
    val deviceStream = deviceStreamJSON.map{ rdd => val deviceStatusEvent = JSON.parseFull(rdd._2).getOrElse("{}").asInstanceOf[Map[String,Any]]
                                                    val serialNumber = deviceStatusEvent.get("serialNumber").get.asInstanceOf[String]
                                                    val internalTemp = deviceStatusEvent.get("internalTemp").get.asInstanceOf[Int].toDouble
                                                    (serialNumber, internalTemp)   
                                            }.updateStateByKey(fillFeatureList).foreachRDD(rdd => {
                                                                                rdd.foreach(featureTupleList =>
                                                                                {
                                                                                  val tempFailPredication = "Temperature pattern indicates imminent device failure. Contact customer or send technician"
                                                                                  val serialNumber = featureTupleList._1
                                                                                  val featureList = featureTupleList._2
                                                                                  if(featureList.size == 10){
                                                                                    println("**********************Feature list has reached required size... " + featureList)
                                                                                    println("**********************Making a prediction...")
                                                                                    val predictionFeatures = Vectors.dense(featureList.toArray)	
							                                                                      val prediction = nostradamus.predict(predictionFeatures)
							                                                                      if(prediction == 1.0){
								                                                                      println("*********************************************************************************")
								                                                                      println("**********************DEVICE FAILURE IMMINENT: " + serialNumber + " : " + prediction + " : " + predictionFeatures)
								                                                                      println("*********************************************************************************")
								                                                                      val httpClient = new HttpClient
								                                                                      httpClient.start
                                                                                      val bayuexClient = new BayeuxClient(cometdUrl.value, new LongPollingTransport( new java.util.HashMap[String,Object](), httpClient))
								                                                                      bayuexClient.handshake
								                                                                      if (bayuexClient.waitFor(3000, BayeuxClient.State.CONNECTED)){
		                                                                                    println("Connected to Cometd Http PubSub Platform")
								                                                                        val data = new java.util.HashMap[String,String]()
								                                                                        data.put("deviceSerialNumber", serialNumber)
								                                                                        data.put("predictionDescription", tempFailPredication)
								                                                                        bayuexClient.getChannel(predictionChannel.value).publish(data)
								                                                                      }else{
			                                                                                  println("Could not connect to Cometd Http PubSub Platform")
								                                                                      }  
							                                                                      }else{
								                                                                      println("*********************************************************************************")
								                                                                      println("**********************DEVICE FUNCTION NORMAL : " + serialNumber + " : " + prediction + " : " + predictionFeatures)
								                                                                      println("*********************************************************************************")
							                                                                     }
                                                                                }else{
                                                                                  println("$$$$$$$$$$$$$$$$$$$$$$$$DEVICE: " + serialNumber + "... Not enough events to make a prediction...")
                                                                                }
                                                                               }
                                                                              )
                                                                             }  
                                                                        )
    ssc.start()
    ssc.awaitTermination()
  }
  
  def fillFeatureList(incomingEventList: Seq[(Double)], currentEventWindow: Option[List[Double]]): Option[List[Double]] = {
    println("@@@@@@@@@@@@@@@@@@@@Current Event List... " + currentEventWindow)
    println("@@@@@@@@@@@@@@@@@@@@Incoming Event List... " + incomingEventList)
    val updatedEventWindow = currentEventWindow.getOrElse(List()).++(incomingEventList)
    println("@@@@@@@@@@@@@@@@@@@@Updated Event List " + updatedEventWindow)
    val returnEventWindow = if(updatedEventWindow.size > 10){
      updatedEventWindow.drop(10)
    }else{ 
      updatedEventWindow
    }
    println("@@@@@@@@@@@@@@@@@@@@Returning Event List " + returnEventWindow)
    Some(returnEventWindow)
  }
}