package com.hortonworks.bolts;

import java.util.Map;

import com.hortonworks.events.DeviceAlert;
import com.hortonworks.events.STBStatus;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class IncidentDetector extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	
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
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream("DeviceStatusStream", new Fields("DeviceStatus"));
		declarer.declareStream("DeviceAlertStream", new Fields("DeviceAlert"));
		declarer.declareStream("DeviceStatusLogStream", new Fields("serialNumber", "deviceModel", "status", "state", "internalTemp", "signalStrength"));
	}

}
