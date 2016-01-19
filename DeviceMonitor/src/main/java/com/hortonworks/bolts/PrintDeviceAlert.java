package com.hortonworks.bolts;

import java.util.Map;

import com.hortonworks.events.DeviceAlert;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PrintDeviceAlert extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	
	public void execute(Tuple tuple) {
		DeviceAlert deviceAlert = (DeviceAlert) tuple.getValueByField("DeviceAlert");
		System.out.println("ALERT******");
		System.out.println("Device Serial Number: " + deviceAlert.getSerialNumber());
		System.out.println("Device Model: " + deviceAlert.getDeviceModel());
		System.out.println("Incident Description: " + deviceAlert.getAlertDescription());
		System.out.println("Device Lat: " + deviceAlert.getLatitude());
		System.out.println("Device Lng: " + deviceAlert.getLongitude());
		
		collector.emit(tuple, new Values((DeviceAlert)deviceAlert));
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("DeviceAlert"));
	}
}
