package com.hortonworks.bolts;

import java.util.Map;

import com.hortonworks.events.STBStatus;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PrintDeviceStatus extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	
	public void execute(Tuple tuple) {
		STBStatus deviceStatus = (STBStatus) tuple.getValueByField("DeviceStatus");
		System.out.println("Device Serial Number: " + deviceStatus.getSerialNumber());
		System.out.println("State: " + deviceStatus.getState());
		System.out.println("Status: " + deviceStatus.getStatus());
		System.out.println("Internal Temp: " + deviceStatus.getInternalTemp());
		System.out.println("Signal Strength: " + deviceStatus.getSignalStrength());
		System.out.println("Device Model: " + deviceStatus.getDeviceModel());
		System.out.println("Latitude: " + deviceStatus.getLatitude());
		System.out.println("Longitude: " + deviceStatus.getLongitude());
		//collector.emit(tuple, new Values((DeviceStatus)deviceStatus));
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("DeviceStatus"));
	}
}
