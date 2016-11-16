package com.hortonworks.iot.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka._
import org.apache.spark.mllib.classification.SVMModel
import scala.util.parsing.json._

object SparkNostradamus {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("SparkNostradamus")
    // Create the context
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    ssc.sparkContext.setLogLevel("WARN")
    ssc.checkpoint("/tmp");
    //val nostradamus = SVMModel.load(ssc.sparkContext, ""+"nostradamusSVMModel");
    
    case class DeviceStatus(
        serialNumber:String, 
        status:String, 
        state:String, 
        deviceModel:String, 
        internalTemp:Int, 
        signalStrength:Int, 
        latitude:Double, 
        longitude:Double)
	
    val kafkaTopicConfig = Map(args(1) -> 1)    
    val deviceStreamJSON = KafkaUtils.createStream(ssc, args(0), "spark-streaming-group", kafkaTopicConfig)
    val deviceStream = deviceStreamJSON.map{ rdd => val deviceStatus = JSON.parseFull(Some(rdd).get._2).get.asInstanceOf[Map[String,Any]]
                                                        val serialNumber = deviceStatus.get("serialNumber").asInstanceOf[String]
                                                        val internalTemp = deviceStatus.get("internalTemp").asInstanceOf[String]
                                                        (serialNumber, internalTemp)   
                                            }
    deviceStream.print()
    //deviceStream.updateStateByKey(updateFunction)
                                 
    
    
    ssc.start()
    ssc.awaitTermination()
  }
  def updateFunction(eventList: Seq[(String)], tempEventWindow: Option[(String)]): Option[(String)] = 
  {
      val eventWindowArray = null
				
     //Check if State is Present
      if(tempEventWindow.isEmpty){
				//Turn state from String to Array
				System.out.println("Temp Event Window: " + tempEventWindow.get.replaceAll("Optional.of\\(", "").replaceAll("\\)", ""));
				System.out.println("Temp Event Window Split Length: " + tempEventWindow.get.split(",").length);
				System.out.println("EventList Size: " + eventList.size)
					
				val eventWindowArrayLength = eventList.size + tempEventWindow.get.split(",").length
				val eventWindowArray = tempEventWindow.get.replaceAll("Optional.of\\(", "").replaceAll("\\)", "").split(",")
				System.out.println("eventWindowArray Length: " + eventWindowArray.length);
					
				var featureWindowList = eventWindowArray.toList
				System.out.println("featureWindowList Length: " + featureWindowList.size);
					
				//If state list size == 10 (expected feature Vector size), clear list and add new events
				if(featureWindowList.size == 10){
				  featureWindowList = List[String]()
					for (event <- eventList){
						featureWindowList.:+(event)	
					}	
			  //If state list size > 10 (expected feature Vector size), remove elements in position <= 10
			  }else if(featureWindowList.size > 10){
					featureWindowList.drop(10)
					//Add new events to state list
					for(event <- eventList){
					  featureWindowList.+:(event);	
			    }
					//If state < 10, add new events to eventWindow array	
			  }else{
			    System.out.println("eventWindowArray Length: " + eventWindowArray.length);
				  for(event <- eventList){
				    featureWindowList.+:(event)	
				  }
			  }
			  System.out.println("featureWindowList: " + featureWindowList.toString);
			  //Convert state list back to Optional String
			  //tempEventWindow = featureWindowList.toString 
			  /*
			  for(int k=0; k<featureWindowList.size(); k++){
				  if(k==0){
					  tempEventWindow = Optional.of(featureWindowList.get(k));
				  }else{
					  tempEventWindow = Optional.of(tempEventWindow + "," + featureWindowList.get(k));
					}
			  }*/
		  }else{
				//If state Optional does not exist, create it	
				//tempEventWindow = 	for(event <- eventList){
		        //tempEventWindow = Optional.of(event);
						//}else{
						//	tempEventWindow = Optional.of(tempEventWindow + "," + eventList.get(i));
						//}
				//}
		 }		
	  tempEventWindow  
  }
}

/*
val deviceStreamJSON = KafkaUtils.createStream(ssc, args(0), "spark-streaming-group", kafkaTopicConfig)
    val deviceStream = deviceStreamJSON.mapValues { rdd => val deviceStatus = JSON.parseFull(rdd).get.asInstanceOf[Map[String,Any]]
                                                        val serialNumber = deviceStatus.get("serialNumber").asInstanceOf[String]
                                                        val status = deviceStatus.get("status").asInstanceOf[String]
                                                        val state = deviceStatus.get("state").asInstanceOf[String]
                                                        val deviceModel = deviceStatus.get("deviceModel").asInstanceOf[String]
                                                        val internalTemp = deviceStatus.get("internalTemp").asInstanceOf[Int]
                                                        val signalStrength = deviceStatus.get("signalStrength").asInstanceOf[Int]
                                                        val latitude = deviceStatus.get("latitude").asInstanceOf[Double]
                                                        val longitude = deviceStatus.get("longitude").asInstanceOf[Double]
                                                        (serialNumber,DeviceStatus( serialNumber,
                                                                      status,
                                                                      state,
                                                                      deviceModel,
                                                                      internalTemp,
                                                                      signalStrength,
                                                                      latitude, 
                                                                      longitude)
                                                        )   
                                                  }
*/