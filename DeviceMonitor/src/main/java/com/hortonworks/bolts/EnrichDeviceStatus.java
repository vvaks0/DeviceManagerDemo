package com.hortonworks.bolts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.hortonworks.events.DeviceStatus;
import com.hortonworks.events.STBStatus;
import com.hortonworks.util.Constants;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

/*
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
*/

public class EnrichDeviceStatus extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	private Constants constants;
	private HTable table = null;
	private Connection conn;
	
	@SuppressWarnings("deprecation")
	public void execute(Tuple tuple) {
		STBStatus deviceStatus = (STBStatus) tuple.getValueByField("DeviceStatus");
		
	    Get get = new Get(Bytes.toBytes(deviceStatus.getSerialNumber()));
	    System.out.println("Build Request...");
	    // Reading the data
	    Result result = null;
		try {
			result = table.get(get);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    //System.out.println("Get Results...");
	    // Reading values from Result class object
		
		if(result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("Longitude")) !=null && result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("Latitude")) != null){
			byte [] deviceModel = result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("DeviceModel"));
			byte [] longitude = result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("Longitude"));
			byte [] latitude = result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("Latitude"));
	    
			deviceStatus.setDeviceModel(Bytes.toString(deviceModel));
			deviceStatus.setLatitude(Double.parseDouble(Bytes.toString(latitude)));
			deviceStatus.setLongitude(Double.parseDouble(Bytes.toString(longitude)));
		
			collector.emit(tuple, new Values((DeviceStatus)deviceStatus));
			collector.ack(tuple);
		}
		else{
			System.out.println("Recieved and event from a device that is not in the datastore or location is unknown... dropping event");
			collector.ack(tuple);
		}
		/*System.out.println(deviceStatus.getSerialNumber());
		System.out.println(deviceStatus.getState());
		System.out.println(deviceStatus.getStatus());
		System.out.println(deviceStatus.getInternalTemp());
		System.out.println(deviceStatus.getSignalStrength());*/
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.constants = new Constants();
		
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
		
		String tableName = "DeviceDetails";
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
			if (hbaseAdmin.tableExists(tableName)) {
				table = new HTable(config, tableName);
				System.out.println("********************** Acquired " + tableName);
				Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
				conn = DriverManager.getConnection("jdbc:phoenix:"+ constants.getZkHost() + ":" + constants.getZkPort() + ":" + constants.getZkHBasePath());
			}else{
				System.out.println("********************** Table " + tableName + "does not exist, creating...");
				Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
				conn = DriverManager.getConnection("jdbc:phoenix:"+ constants.getZkHost() + ":" + constants.getZkPort() + ":" + constants.getZkHBasePath());
				conn.createStatement().execute("CREATE TABLE IF NOT EXISTS \"DeviceDetailsBI\" "
						+ "(\"SerialNumber\" VARCHAR NOT NULL PRIMARY KEY, "
						+ "\"DeviceModel\" VARCHAR, "
						+ "\"Latitude\" VARCHAR, "
						+ "\"Longitude\" VARCHAR, "
						+ "\"IpAddress\" VARCHAR, "
						+ "\"Port\" VARCHAR, ");
				conn.commit();
				
				conn.createStatement().executeUpdate("UPSERT INTO \"DeviceDetailsBI\" VALUES("
						+ "'Motorolla', "
						+ "'39.951694', "
						+ "'-75.144596', "
						+ "'192.168.56.1', "
						+ "'8085')");
				conn.commit();
				
				conn.createStatement().executeUpdate("UPSERT INTO \"DeviceDetailsBI\" VALUES("
						+ "'Motorolla', "
						+ "'39.951694', "
						+ "'-75.144596', "
						+ "'192.168.56.1', "
						+ "'8087')");
				conn.commit();
				
				conn.createStatement().executeUpdate("UPSERT INTO \"DeviceDetailsBI\" VALUES("
						+ "'Motorolla', "
						+ "'39.951694', "
						+ "'-75.144596', "
						+ "'192.168.56.1', "
						+ "'8089')");
				conn.commit();
				
				HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
				HColumnDescriptor cfColumnFamily = new HColumnDescriptor("cf".getBytes());
		        tableDescriptor.addFamily(cfColumnFamily);
		        hbaseAdmin.createTable(tableDescriptor);
		        System.out.println("********************** Created " + tableName);
		        table = new HTable(config, tableName);
		        System.out.println("********************** Acquired " + tableName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Put device1000 = new Put(Bytes.toBytes("1000"));
		device1000.add(Bytes.toBytes("cf"), Bytes.toBytes("DeviceModel"), Bytes.toBytes("Motorolla"));
		device1000.add(Bytes.toBytes("cf"), Bytes.toBytes("Latitude"), Bytes.toBytes("39.951694"));
		device1000.add(Bytes.toBytes("cf"), Bytes.toBytes("Longitude"), Bytes.toBytes("-75.144596"));
		device1000.add(Bytes.toBytes("cf"), Bytes.toBytes("IpAddress"), Bytes.toBytes("192.168.56.1"));
		device1000.add(Bytes.toBytes("cf"), Bytes.toBytes("Port"), Bytes.toBytes("8085"));
		
		Put device2000 = new Put(Bytes.toBytes("2000"));
		device2000.add(Bytes.toBytes("cf"), Bytes.toBytes("DeviceModel"), Bytes.toBytes("Motorolla"));
		device2000.add(Bytes.toBytes("cf"), Bytes.toBytes("Latitude"), Bytes.toBytes("39.970279"));
		device2000.add(Bytes.toBytes("cf"), Bytes.toBytes("Longitude"), Bytes.toBytes("-75.175152"));
		device2000.add(Bytes.toBytes("cf"), Bytes.toBytes("IpAddress"), Bytes.toBytes("192.168.56.1"));
		device2000.add(Bytes.toBytes("cf"), Bytes.toBytes("Port"), Bytes.toBytes("8087"));
		
		Put device3000 = new Put(Bytes.toBytes("3000"));
		device3000.add(Bytes.toBytes("cf"), Bytes.toBytes("DeviceModel"), Bytes.toBytes("Motorolla"));
		device3000.add(Bytes.toBytes("cf"), Bytes.toBytes("Latitude"), Bytes.toBytes("39.948174"));
		device3000.add(Bytes.toBytes("cf"), Bytes.toBytes("Longitude"), Bytes.toBytes("-75.170689"));
		device3000.add(Bytes.toBytes("cf"), Bytes.toBytes("IpAddress"), Bytes.toBytes("192.168.56.1"));
		device3000.add(Bytes.toBytes("cf"), Bytes.toBytes("Port"), Bytes.toBytes("8089"));
		
		try {
			table.put(device1000);
			table.put(device2000);
			table.put(device3000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("DeviceStatus"));
	}
}