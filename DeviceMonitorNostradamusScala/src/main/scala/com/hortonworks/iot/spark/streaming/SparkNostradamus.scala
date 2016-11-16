package com.hortonworks.iot.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka._
import org.apache.spark.mllib.classification.SVMModel
import org.apache.spark.mllib.linalg.Vectors
import scala.util.parsing.json._

object SparkNostradamus {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("SparkNostradamus")
    // Create the context
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    ssc.sparkContext.setLogLevel("WARN")
    ssc.checkpoint("/demo/data/checkpoint");
    val nostradamus = SVMModel.load(ssc.sparkContext, "/demo/data/model/nostradamusSVMModel");

    val kafkaTopicConfig = Map(args(1) -> 1)    
    val deviceStreamJSON = KafkaUtils.createStream(ssc, args(0), "spark-streaming-group", kafkaTopicConfig)
    deviceStreamJSON.foreachRDD(_.collect().foreach(println))
    val deviceStream = deviceStreamJSON.map{ rdd => val deviceStatusEvent = JSON.parseFull(rdd._2).getOrElse("{}").asInstanceOf[Map[String,Any]]
                                                    val serialNumber = deviceStatusEvent.get("serialNumber").get.asInstanceOf[String]
                                                    val internalTemp = deviceStatusEvent.get("internalTemp").get.asInstanceOf[Int].toDouble
                                                    (serialNumber, internalTemp)   
                                            }
    deviceStream.foreachRDD(_.collect().foreach(println))
    deviceStream.updateStateByKey(fillFeatureList).map(_._2).foreachRDD(rdd => {
                                                                                rdd.map(x => x.toArray.take(10)).foreach(featureList =>
                                                                                {
                                                                                  if(featureList.size == 10){
                                                                                    println("Feature list has reached required size... " + featureList)
                                                                                    println("Making a prediction...")
                                                                                    val predictionFeatures = Vectors.dense(featureList)	
							                                                                      val prediction = nostradamus.predict(predictionFeatures)
							                                                                      if(prediction == 1.0){
								                                                                      println("*********************************************************************************");
								                                                                      println("**********************DEVICE FAILURE IMMINENT: " + prediction + predictionFeatures);
								                                                                      println("*********************************************************************************");
							                                                                      }else{
								                                                                      println("*********************************************************************************");
								                                                                      println("**********************DEVICE FUNCTION NORMAL : " + prediction + predictionFeatures);
								                                                                      println("*********************************************************************************");
							                                                                     }
                                                                                }else{
                                                                                  println("Not enough events to make a prediction...")
                                                                                }
                                                                               }
                                                                              )
                                                                             }  

                                                                        )
    ssc.start()
    ssc.awaitTermination()
  }
  
  def fillFeatureList(incomingEventList: Seq[(Double)], currentEventWindow: Option[List[Double]]): Option[List[Double]] = {
    println("Current Event List... " + currentEventWindow)
    println("Incoming Event List... " + incomingEventList)
    val updatedEventWindow = currentEventWindow.getOrElse(List()).++(incomingEventList)
    println("Updated Event List " + updatedEventWindow)
    val returnEventWindow = if(updatedEventWindow.size > 10){
      updatedEventWindow.drop(10)
    }else{ 
      updatedEventWindow
    }
    println("Returning Event List " + returnEventWindow)
    Some(returnEventWindow)
  }
}
/*
case class DeviceStatus(
        serialNumber:String, 
        status:String, 
        state:String, 
        deviceModel:String, 
        internalTemp:Int, 
        signalStrength:Int, 
        latitude:Double, 
        longitude:Double)
        
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