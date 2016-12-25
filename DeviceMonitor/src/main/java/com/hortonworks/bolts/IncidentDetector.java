package com.hortonworks.bolts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import com.hortonworks.events.DeviceAlert;
import com.hortonworks.events.STBStatus;
import com.hortonworks.util.Constants;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;

/*
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
*/

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

public class IncidentDetector extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	private Constants constants;
	private Connection conn;
	
	public void execute(Tuple tuple) {
		STBStatus deviceStatus = (STBStatus) tuple.getValueByField("DeviceStatus");
		collector.emit("DeviceStatusLogStream", new Values(deviceStatus.getSerialNumber(), deviceStatus.getDeviceModel(), deviceStatus.getStatus(), deviceStatus.getState(), deviceStatus.getInternalTemp(), deviceStatus.getSignalStrength()));
		if(deviceStatus.getSignalStrength() <= 70){
			DeviceAlert signalStrengthAlert = new DeviceAlert();
			signalStrengthAlert.setAlertDescription("Signal Strength is too low");
			signalStrengthAlert.setSerialNumber(deviceStatus.getSerialNumber());
			signalStrengthAlert.setLongitude(deviceStatus.getLongitude());
			signalStrengthAlert.setLatitude(deviceStatus.getLatitude());
			collector.emit("DeviceAlertStream", new Values(signalStrengthAlert));
		}
		else if(deviceStatus.getInternalTemp() >= 105){
			DeviceAlert internalTempAlert = new DeviceAlert();
			internalTempAlert.setAlertDescription("Device internal temp is too high");
			internalTempAlert.setSerialNumber(deviceStatus.getSerialNumber());
			internalTempAlert.setDeviceModel(deviceStatus.getDeviceModel());
			internalTempAlert.setLongitude(deviceStatus.getLongitude());
			internalTempAlert.setLatitude(deviceStatus.getLatitude());
			collector.emit("DeviceAlertStream", new Values(internalTempAlert));
		}
		else{
			collector.emit("DeviceStatusStream", new Values(deviceStatus));
		}
		
		try {
			conn.createStatement().executeUpdate("UPSERT INTO \"DeviceStatusLog\" VALUES("
					+ "'"+deviceStatus.getSerialNumber()+"', "
					+ "'"+deviceStatus.getStatus()+"', "
					+ "'"+deviceStatus.getState()+"', "
					+ ""+deviceStatus.getInternalTemp()+", "
					+ ""+deviceStatus.getSignalStrength()+")");
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
		constants = new Constants();
		System.out.println("********************** Zookeeper Host: " + constants.getZkHost());
        System.out.println("********************** Zookeeper Port: " + constants.getZkPort());
        System.out.println("********************** Zookeeper ConnString: " + constants.getZkConnString());
        System.out.println("********************** Zookeeper Kafka Path: " + constants.getZkKafkaPath());
        System.out.println("********************** Zookeeper HBase Path: " + constants.getZkHBasePath());
        System.out.println("********************** Cometd URI: " + constants.getPubSubUrl());
		
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", constants.getZkHost());
		config.set("hbase.zookeeper.property.clientPort", constants.getZkPort());
		config.set("zookeeper.znode.parent", constants.getZkHBasePath());
		
		String tableName = "DeviceStatusLog";
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
			if (hbaseAdmin.tableExists("tableName")) {
				System.out.println("********************** Acquired " + tableName);
				Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
				conn = DriverManager.getConnection("jdbc:phoenix:"+ constants.getZkHost() + ":" + constants.getZkPort() + ":" + constants.getZkHBasePath());
			}else{
				System.out.println("********************** Table " + tableName + "does not exist, creating...");
				Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
				conn = DriverManager.getConnection("jdbc:phoenix:"+ constants.getZkHost() + ":" + constants.getZkPort() + ":" + constants.getZkHBasePath());
				conn.createStatement().execute("CREATE TABLE IF NOT EXISTS \"DeviceStatusLog\" "
						+ "(\"serialNumber\" VARCHAR NOT NULL PRIMARY KEY, "
						+ "(\"state\" VARCHAR, "
						+ "\"status\" VARCHAR, "
						+ "\"internalTemp\" INT, "
						+ "\"signalStrength\" INT");
				conn.commit();
				
		        System.out.println("********************** Created " + tableName);
		        System.out.println("********************** Acquired " + tableName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("DeviceStatusStream", new Fields("DeviceStatus"));
		declarer.declareStream("DeviceAlertStream", new Fields("DeviceAlert"));
		declarer.declareStream("DeviceStatusLogStream", new Fields("serialNumber", "deviceModel", "status", "state", "internalTemp", "signalStrength"));
		declarer.declareStream("DeviceStatusLogNullStream", new Fields("serialNumber", "deviceModel", "status", "state", "internalTemp", "signalStrength"));
	}

}
