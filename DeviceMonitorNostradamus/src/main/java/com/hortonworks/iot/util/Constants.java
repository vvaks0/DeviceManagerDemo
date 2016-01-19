package com.hortonworks.iot.util;

import java.util.List;

import com.google.common.collect.Lists;

public class Constants {	
		//Properties file which has all the configurable parameters required for execution of this Topology.
		public static final String CONFIG_PROPERTIES_FILE = "config.properties";
		
		public static final String zkConnString = "sandbox.hortonworks.com:2181";
		public static final String deviceTopicName = "DeviceEvents";
		public static final String technicianTopicName = "TechnicianEvent";
		
		public static final String pubSubUrl = "http://sandbox.hortonworks.com:8091/cometd";
		public static final String technicianChannel = "/technicianstatus";
		public static final String deviceChannel = "/devicestatus";
		public static final String alertChannel = "/alert";
		public static final String predictionChannel = "/prediction";
		
		public static final String nameNode = "hdfs://sandbox.hortonworks.com:8020";
		public static final String hivePath = "/demo/data/device_logs";
		public static final String sparkModelPath = "/demo/data/model/";
		public static final String sparkCheckpointPath = "/demo/data/checkpoints/";
}