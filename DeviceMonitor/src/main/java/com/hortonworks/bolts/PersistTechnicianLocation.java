package com.hortonworks.bolts;

import java.io.IOException;
import java.util.HashMap;
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

import com.hortonworks.events.TechnicianStatus;
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

public class PersistTechnicianLocation extends BaseRichBolt {

	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	private Constants constants;
	private HTable table = null;
	
	@SuppressWarnings("deprecation")
	public void execute(Tuple tuple) {
		TechnicianStatus technicianStatus = (TechnicianStatus) tuple.getValueByField("TechnicianStatus");

		Put p = new Put(Bytes.toBytes(technicianStatus.getTechnicianId()));
		p.add(Bytes.toBytes("cf"), Bytes.toBytes("Status"), Bytes.toBytes(technicianStatus.getStatus()));
		p.add(Bytes.toBytes("cf"), Bytes.toBytes("Latitude"), Bytes.toBytes(technicianStatus.getLatitude().toString()));
		p.add(Bytes.toBytes("cf"), Bytes.toBytes("Longitude"), Bytes.toBytes(technicianStatus.getLongitude().toString()));
		p.add(Bytes.toBytes("cf"), Bytes.toBytes("IpAddress"), Bytes.toBytes(technicianStatus.getIpAddress().toString()));
		p.add(Bytes.toBytes("cf"), Bytes.toBytes("Port"), Bytes.toBytes(technicianStatus.getPort().toString()));
		try {
			table.put(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("TechnicianId: " + technicianStatus.getTechnicianId());
		System.out.println("Technician Status: " + technicianStatus.getStatus());
		System.out.println("Latitude: " + technicianStatus.getLatitude());
		System.out.println("Longitude: " + technicianStatus.getLongitude());
		System.out.println("IpAddress: " + technicianStatus.getIpAddress());
		System.out.println("Port: " + technicianStatus.getPort());
		
		collector.emit(tuple, new Values((TechnicianStatus)technicianStatus));
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
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
		
		String tableName = "TechnicianEvents";
		try {
			HBaseAdmin hbaseAdmin = new HBaseAdmin(config);
			if (hbaseAdmin.tableExists(tableName)) {
				table = new HTable(config, tableName);
			}else{
				System.out.println("********************** Table " + tableName + "does not exist, creating...");
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
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("TechnicianStatus"));
	}
}