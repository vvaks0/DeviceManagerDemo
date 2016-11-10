package com.hortonworks.util;

import java.util.Map;

public class Constants {
	private String zkHost = "sandbox.hortonworks.com";
	private String zkPort = "2181";
	private String zkKafkaPath = "/brokers";
	private String zkHBasePath = "/hbase-unsecure";
	private String zkConnString;
	private String deviceEventsTopicName = "DeviceEvents";
	private String technicianEventsTopicName = "TechnicianEvent";
	
	private String cometdHost = "sandbox.hortonworks.com";
	private String cometdPort = "8091";
	private String pubSubUrl;
	private String alertChannel = "/alert";
	private String deviceChannel = "/devicestatus";
	private String technicianChannel = "/technicianstatus";
	
	private String nameNodeHost = "sandbox.hortonworks.com";
	private String nameNodePort = "8020";
	private String nameNodeUrl;
	private String hivePath = "/demo/data/transaction_logs";
	private String hbasePath = "/apps/hbase/data/";
	
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
        	this.zkKafkaPath = (String)env.get("ZK_KAFKA_PATH");
        }
        if(env.get("ZK_HBASE_PATH") != null){
        	this.zkHBasePath = (String)env.get("ZK_HBASE_PATH");
        }
        if(env.get("NAMENODE_HOST") != null){
        	this.setNameNodeHost((String)env.get("NAMENODE_HOST"));
        }
        if(env.get("NAMENODE_PORT") != null){
        	this.setNameNodePort((String)env.get("NAMENODE_PORT"));
        }
        if(env.get("HIVE_PATH") != null){
        	this.hivePath = (String)env.get("HIVE_PATH");
        }
        if(env.get("HIVE_METASTORE_URI") != null){
        	this.hiveMetaStoreURI = (String)env.get("HIVE_METASTORE_URI");
        }
        if(env.get("ATLAS_HOST") != null){
        	this.setAtlasHost((String)env.get("ATLAS_HOST"));
        }
        if(env.get("ATLAS_PORT") != null){
        	this.setAtlasPort((String)env.get("ATLAS_PORT"));
        }
        if(env.get("COMETD_HOST") != null){
        	this.cometdHost = (String)env.get("COMETD_HOST");
        }
        if(env.get("COMETD_PORT") != null){
        	this.cometdPort = (String)env.get("COMETD_PORT");
        }
        
        this.zkConnString = zkHost+":"+zkPort;
        this.pubSubUrl = "http://" + cometdHost + ":" + cometdPort + "/cometd";
        this.nameNodeUrl = "hdfs://" + nameNodeHost + ":" + nameNodePort;
	}

	public String getZkHost() {
		return zkHost;
	}

	public void setZkHost(String zkHost) {
		this.zkHost = zkHost;
	}
	
	public String getZkPort() {
		return zkPort;
	}

	public void setZkPort(String zkPort) {
		this.zkPort = zkPort;
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

	public String getZkConnString() {
		return zkConnString;
	}

	public void setZkConnString(String zkConnString) {
		this.zkConnString = zkConnString;
	}

	public String getPubSubUrl() {
		return pubSubUrl;
	}

	public void setPubSubUrl(String pubSubUrl) {
		this.pubSubUrl = pubSubUrl;
	}

	public String getHivePath() {
		return hivePath;
	}

	public void setHivePath(String hivePath) {
		this.hivePath = hivePath;
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

	public String getNameNodeUrl() {
		return nameNodeUrl;
	}

	public void setNameNodeUrl(String nameNodeUrl) {
		this.nameNodeUrl = nameNodeUrl;
	}

	public String getNameNodePort() {
		return nameNodePort;
	}

	public void setNameNodePort(String nameNodePort) {
		this.nameNodePort = nameNodePort;
	}

	public String getNameNodeHost() {
		return nameNodeHost;
	}

	public void setNameNodeHost(String nameNodeHost) {
		this.nameNodeHost = nameNodeHost;
	}

	public String getHbasePath() {
		return hbasePath;
	}

	public void setHbasePath(String hbasePath) {
		this.hbasePath = hbasePath;
	}

	public String getAlertChannel() {
		return alertChannel;
	}

	public void setAlertChannel(String alertChannel) {
		this.alertChannel = alertChannel;
	}

	public String getDeviceChannel() {
		return deviceChannel;
	}

	public void setDeviceChannel(String deviceChannel) {
		this.deviceChannel = deviceChannel;
	}

	public String getTechnicianChannel() {
		return technicianChannel;
	}

	public void setTechnicianChannel(String technicianChannel) {
		this.technicianChannel = technicianChannel;
	}

	public String getDeviceEventsTopicName() {
		return deviceEventsTopicName;
	}

	public void setDeviceEventsTopicName(String deviceEventsTopicName) {
		this.deviceEventsTopicName = deviceEventsTopicName;
	}

	public String getTechnicianEventsTopicName() {
		return technicianEventsTopicName;
	}

	public void setTechnicianEventsTopicName(String technicianEventsTopicName) {
		this.technicianEventsTopicName = technicianEventsTopicName;
	}
}
