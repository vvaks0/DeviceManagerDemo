package com.hortonworks.iot.util;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

public class Constants {	
		//Properties file which has all the configurable parameters required for execution of this Topology.
		public static final String CONFIG_PROPERTIES_FILE = "config.properties";
		
		private String zkHost = "sandbox.hortonworks.com";
		private String zkPort = "2181";
		private String zkKafkaPath = "/brokers";
		private String zkHBasePath = "/hbase-unsecure";
		private String zkConnString = "sandbox.hortonworks.com:2181";
		private String deviceTopicName = "DeviceEvents";
		private String technicianTopicName = "TechnicianEvent";
		
		private String cometdHost = "sandbox.hortonworks.com";
		private String cometdPort = "8091";
		private String pubSubUrl;
		private String technicianChannel = "/technicianstatus";
		private String deviceChannel = "/devicestatus";
		private String alertChannel = "/alert";
		private String predictionChannel = "/prediction";
		
		private String nameNodeHost = "sandbox.hortonworks.com";
		private String nameNodePort = "8020";
		private String nameNodeUrl;
		private String hivePath = "/demo/data/device_logs";
		private String sparkModelPath = "/demo/data/model/";
		private String sparkCheckpointPath = "/demo/data/checkpoints/";
		
		private String atlasHost = "localhost";
		private String atlasPort = "21000";
		
		private String hiveMetaStoreURI = "jdbc:mysql://sandbox.hortonworks.com/hive";
		private String hiveDbName = "default";
		
		public Constants(){
			Map<String, String> env = System.getenv();
	        //System.out.println("********************** ENV: " + env);
	        if(env.get("ZK_HOST") != null){
	        	this.zkHost = (String)env.get("ZK_HOST");
	        }
	        if(env.get("ZK_PORT") != null){
	        	this.zkPort = (String)env.get("ZK_PORT");
	        }
	        if(env.get("ZK_KAFKA_PATH") != null){
	        	this.setZkKafkaPath((String)env.get("ZK_KAFKA_PATH"));
	        }
	        if(env.get("ZK_HBASE_PATH") != null){
	        	this.setZkHBasePath((String)env.get("ZK_HBASE_PATH"));
	        }
	        if(env.get("NAMENODE_HOST") != null){
	        	this.setNameNodeHost((String)env.get("NAMENODE_HOST"));
	        }
	        if(env.get("NAMENODE_PORT") != null){
	        	this.setNameNodePort((String)env.get("NAMENODE_PORT"));
	        }
	        if(env.get("HIVE_PATH") != null){
	        	this.setHivePath((String)env.get("HIVE_PATH"));
	        }
	        if(env.get("HIVE_METASTORE_URI") != null){
	        	this.setHiveMetaStoreURI((String)env.get("HIVE_METASTORE_URI"));
	        }
	        if(env.get("ATLAS_HOST") != null){
	        	this.setAtlasHost((String)env.get("ATLAS_HOST"));
	        }
	        if(env.get("ATLAS_PORT") != null){
	        	this.setAtlasPort((String)env.get("ATLAS_PORT"));
	        }
	        if(env.get("COMETD_HOST") != null){
	        	this.setCometdHost((String)env.get("COMETD_HOST"));
	        }
	        if(env.get("COMETD_PORT") != null){
	        	this.setCometdPort((String)env.get("COMETD_PORT"));
	        }
	        
	        this.setZkConnString(zkHost+":"+zkPort);
	        this.setPubSubUrl("http://" + cometdHost + ":" + cometdPort + "/cometd");
	        this.setNameNodeUrl("hdfs://" + nameNodeHost + ":" + nameNodePort);
		}

		public String getZkKafkaPath() {
			return zkKafkaPath;
		}

		public void setZkKafkaPath(String zkKafkaPath) {
			this.zkKafkaPath = zkKafkaPath;
		}

		public String getZkHBasePath() {
			return zkHBasePath;
		}

		public void setZkHBasePath(String zkHBasePath) {
			this.zkHBasePath = zkHBasePath;
		}

		public String getPubSubUrl() {
			return pubSubUrl;
		}

		public void setPubSubUrl(String pubSubUrl) {
			this.pubSubUrl = pubSubUrl;
		}

		public String getTechnicianTopicName() {
			return technicianTopicName;
		}

		public void setTechnicianTopicName(String technicianTopicName) {
			this.technicianTopicName = technicianTopicName;
		}

		public String getDeviceTopicName() {
			return deviceTopicName;
		}

		public void setDeviceTopicName(String deviceTopicName) {
			this.deviceTopicName = deviceTopicName;
		}

		public String getZkConnString() {
			return zkConnString;
		}

		public void setZkConnString(String zkConnString) {
			this.zkConnString = zkConnString;
		}

		public String getHivePath() {
			return hivePath;
		}

		public void setHivePath(String hivePath) {
			this.hivePath = hivePath;
		}

		public String getSparkModelPath() {
			return sparkModelPath;
		}

		public void setSparkModelPath(String sparkModelPath) {
			this.sparkModelPath = sparkModelPath;
		}

		public String getSparkCheckpointPath() {
			return sparkCheckpointPath;
		}

		public void setSparkCheckpointPath(String sparkCheckpointPath) {
			this.sparkCheckpointPath = sparkCheckpointPath;
		}

		public String getNameNodeUrl() {
			return nameNodeUrl;
		}

		public void setNameNodeUrl(String nameNodeUrl) {
			this.nameNodeUrl = nameNodeUrl;
		}

		public String getAtlasHost() {
			return atlasHost;
		}

		public void setAtlasHost(String atlasHost) {
			this.atlasHost = atlasHost;
		}

		public String getAtlasPort() {
			return atlasPort;
		}

		public void setAtlasPort(String atlasPort) {
			this.atlasPort = atlasPort;
		}

		public String getHiveMetaStoreURI() {
			return hiveMetaStoreURI;
		}

		public void setHiveMetaStoreURI(String hiveMetaStoreURI) {
			this.hiveMetaStoreURI = hiveMetaStoreURI;
		}

		public String getHiveDbName() {
			return hiveDbName;
		}

		public void setHiveDbName(String hiveDbName) {
			this.hiveDbName = hiveDbName;
		}

		public String getTechnicianChannel() {
			return technicianChannel;
		}

		public void setTechnicianChannel(String technicianChannel) {
			this.technicianChannel = technicianChannel;
		}

		public String getAlertChannel() {
			return alertChannel;
		}

		public void setAlertChannel(String alertChannel) {
			this.alertChannel = alertChannel;
		}

		public String getPredictionChannel() {
			return predictionChannel;
		}

		public void setPredictionChannel(String predictionChannel) {
			this.predictionChannel = predictionChannel;
		}

		public String getDeviceChannel() {
			return deviceChannel;
		}

		public void setDeviceChannel(String deviceChannel) {
			this.deviceChannel = deviceChannel;
		}

		public String getCometdHost() {
			return cometdHost;
		}

		public void setCometdHost(String cometdHost) {
			this.cometdHost = cometdHost;
		}

		public String getCometdPort() {
			return cometdPort;
		}

		public void setCometdPort(String cometdPort) {
			this.cometdPort = cometdPort;
		}

		public String getNameNodeHost() {
			return nameNodeHost;
		}

		public void setNameNodeHost(String nameNodeHost) {
			this.nameNodeHost = nameNodeHost;
		}

		public String getNameNodePort() {
			return nameNodePort;
		}

		public void setNameNodePort(String nameNodePort) {
			this.nameNodePort = nameNodePort;
		}
}