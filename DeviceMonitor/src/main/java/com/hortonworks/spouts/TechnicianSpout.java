package com.hortonworks.spouts;

import com.google.appengine.api.search.GeoPoint;
import com.hortonworks.beans.Technician;
import com.hortonworks.events.ExternalRequest;
import com.hortonworks.events.TechnicianDestination;
import com.hortonworks.events.TechnicianStatus;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class TechnicianSpout extends BaseRichSpout {
	private static final long serialVersionUID = 1L;
    private String technicianId;
    private String origin;
    private String destination;
    private String waypointString;
    private Technician technician;
    private Map eventCache;
    private Thread technicianThread;
	private SpoutOutputCollector _collector;
	private LinkedBlockingQueue<TechnicianStatus> queue = null;

    public TechnicianSpout() {}

    public void run() {
        /*ExternalRequest externalRequest = new ExternalRequest();
        while(true){
                if(eventCache.containsKey(technicianId)){
                    externalRequest = (ExternalRequest)eventCache.get(technicianId);
                    switch(externalRequest.getCommand()){
                        case "StartRoute":
                            startRoute();
                        break;
                        case "StopRoute":
                            stopRoute();
                        break;
                    }
                    eventCache.remove(technicianId);
                }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }*/
    }
    
    public void startRoute(){
        System.out.println("Starting Technician Thread ************************");
        technician = new Technician(this);
        technicianThread = new Thread(technician);
        technicianThread.setName("Technician: " + technicianId);
        technicianThread.start();
    }
    
    public void stopRoute(){
        technicianThread.interrupt();
    }

    public void setMap(Map map){
        eventCache = map;
    }
    public void setTechnicianId(String value){
        technicianId = value;
    }
    public void setOrigin(String value){
        origin = value;
    }
    public void setDestination(String value){
        destination = value;
    }
    public void setWaypointString(String value){
        waypointString = value;
    }
    public String getTechnicianId(){
        return technicianId;
    }
    public String getOrigin(){
        return origin;
    }
    public String getDestination(){
        return destination;
    }
    public String getWaypointString(){
        return waypointString;
    }

	public void enqueueEvent(TechnicianStatus status){
		queue.add(status);
	}
	
	public void nextTuple() {
		TechnicianStatus techStatus = queue.poll();
	    if(((TechnicianDestination)techStatus).getTechnicianId().equalsIgnoreCase(technician.getTechnicianId()) && !technician.getAssigned()){
            technician.addTechnicianDestination((TechnicianDestination)techStatus);
            technician.setStatus("Assigned");
        }
		
		if (techStatus == null) {
			Utils.sleep(50);
		} else {
			_collector.emit(new Values(techStatus));
		}
		//System.out.println("Event Sender: " + event.getTechnicianId() + " " + event.getLatitude() + " " + event.getLongitude());
	}

	public void open(Map arg0, TopologyContext arg1, SpoutOutputCollector collector) {
		queue = new LinkedBlockingQueue<TechnicianStatus>(1000);
		_collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer arg0) {
		// TODO Auto-generated method stub
		
	}
}