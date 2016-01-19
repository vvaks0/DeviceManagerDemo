package com.hortonworks.spouts;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import com.hortonworks.beans.STB;
import com.hortonworks.events.DeviceStatus;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

public class DeviceSpout extends BaseRichSpout {
	private static final long serialVersionUID = 1L;
	private String serialNumber;
	private STB stb;
    private Map eventCache;
    private Thread deviceThread;
	private SpoutOutputCollector _collector;
	private LinkedBlockingQueue<DeviceStatus> queue = null;
	
	public void nextTuple() {
		DeviceStatus deviceStatus = queue.poll();
		if (deviceStatus == null) {
			Utils.sleep(50);
		} else {
			_collector.emit(new Values(deviceStatus));
		}
	}

	public void open(Map arg0, TopologyContext context, SpoutOutputCollector collector) {
		queue = new LinkedBlockingQueue<DeviceStatus>(1000);
		_collector = collector;
		
		stb = new STB(this);
        deviceThread = new Thread(stb);
        deviceThread.setName("Device: " + serialNumber);
        deviceThread.start();
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("DeviceStatus"));
	}
	
	public String getSerialNumber(){
		return serialNumber;
	}
	public void enqueueEvent(DeviceStatus status){
		queue.add(status);
	}
}
