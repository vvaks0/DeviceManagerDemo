package com.hortonworks.util;

import java.util.List;

import com.google.common.collect.Lists;

public class Constants {	
		//Properties file which has all the configurable parameters required for execution of this Topology.
		public static final String CONFIG_PROPERTIES_FILE = "config.properties";
		
		public static final String consumerKey = "7KTIA0z1IwREttAcqivmVhucD"; 
		public static final String consumerSecret = "wwJrqTPGJiEUCrNASHWqZhQ00B8c6SvEpcOsAKaMKHqVEjUXGZ"; 
		public static final String accessToken = "2880047437-2zvj4A9OZrz6kKlFE6KB0tOEsCucLsWUHQsArQi"; 
		public static final String accessTokenSecret = "aRjSAOYV1Z4q6WqzlsJOgSAoflIaUVCQRQPAofw0962kq";
        
		/*
        public static final String consumerKey = "gnu96frH371Khn2Vj9DYnIksv"; 
        public static final String consumerSecret = "jJGNP964UPcLjBahO6TUpZPSHH5kGFgCz8Fg1SixpqY0NPHFpJ"; 
        public static final String accessToken = "3630842418-UB4Je1BAMXQWRfSsQAEHxGKKe8PGWVZziJJNyJ9"; 
        public static final String accessTokenSecret = "1uu2v5SuGIY65bwyyDr4RXKh0NfknSKYWacDoujrdcPo7";
        */
		public static final String zkHost = "sandbox.hortonworks.com";
		public static final String zkPort = "2181";
		public static final String zkConnString = zkHost+":"+zkPort;
		public static final String deviceTopicName = "DeviceEvents";
		public static final String technicianTopicName = "TechnicianEvent";
		
		public static final String pubSubUrl = "http://sandbox.hortonworks.com:8091/cometd";
		public static final String technicianChannel = "/technicianstatus";
		public static final String deviceChannel = "/devicestatus";
		public static final String alertChannel = "/alert";
		
		public static final String nameNode = "hdfs://sandbox.hortonworks.com:8020";
		public static final String hivePath = "/demo/data/device_logs";
		
		//Sentiment scores of few words are present in this file.
		//For more info on this, please check: http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010
		public static String AFINN_SENTIMENT_FILE_NAME = "AFINN-111.txt";

		//Codes of all the states of USA.
		public static List<String> CONSOLIDATED_STATE_CODES = Lists.newArrayList("AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","MD","MA","MI","MN","MS","MO","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY");

}
