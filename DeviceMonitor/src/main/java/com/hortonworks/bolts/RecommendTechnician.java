package com.hortonworks.bolts;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import com.hortonworks.events.DeviceAlert;
import com.hortonworks.events.DeviceStatus;
import com.hortonworks.events.TechnicianDestination;
import com.hortonworks.events.TechnicianStatus;

public class RecommendTechnician extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	private OutputCollector collector;
	
    public RecommendTechnician() {}

    public static Double distFrom(Double lat1, Double lng1, Double lat2, Double lng2) {
       double earthRadius = 6371000; //meters
       double dLat = Math.toRadians(lat2-lat1);
       double dLng = Math.toRadians(lng2-lng1);
       double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLng/2) * Math.sin(dLng/2);
       double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
       double dist = (Double)(earthRadius * c);

       return dist;
    }
    
    @SuppressWarnings("deprecation")
	public TechnicianDestination nominateTechnician (DeviceAlert deviceAlert) throws IOException {
    	TechnicianDestination techDestination = new TechnicianDestination();
    	TechnicianStatus currentTechStatus = new TechnicianStatus();
    	TechnicianStatus recommendedTechStatus = new TechnicianStatus();
    	ResultScanner techLocationScanner;
    	
    	Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "localhost");
		config.set("hbase.zookeeper.property.clientPort", "2181");
		config.set("zookeeper.znode.parent", "/hbase-unsecure");		

	    HTable table = null;
		try {
			table = new HTable(config, "TechnicianEvents");
		} catch (IOException e) {
			e.printStackTrace();
		}

	    Scan scan = new Scan();
	    scan.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("Status"));
	    scan.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("Latitude"));
	    scan.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("Longitude"));
	    scan.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("IpAddress"));
	    scan.addColumn(Bytes.toBytes("cf"),Bytes.toBytes("Port"));
	    
	    ResultScanner scanner = table.getScanner(scan);
	    techLocationScanner = scanner;
	    // Scanning the required columns
	    /*for (Result result = scanner.next(); (result != null); result = scanner.next()) {
	    	System.out.println("Row Key: " + Bytes.toString(result.getRow()));
	    	for(KeyValue keyValue : result.list()) {
	            System.out.println("Qualifier : " + Bytes.toString(keyValue.getQualifier()) + " : Value : " + Bytes.toString(keyValue.getValue()));
	        }
	    }*/
	    
	    Double currentDistance = null;
        Double leastDistance = null;        
        System.out.println("********************** DEVICE ALERT: " + deviceAlert.getLatitude() + " , " + deviceAlert.getLongitude());
        System.out.println("Recommended Tech: " + recommendedTechStatus.getTechnicianId());
        System.out.println("Recommended Tech Destination: " + techDestination.getTechnicianId());
        
        for(Result result = techLocationScanner.next(); (result != null); result = techLocationScanner.next()) {
    		currentTechStatus.setTechnicianId(Bytes.toString(result.getRow()));
    		System.out.println(currentTechStatus.getTechnicianId());
    		System.out.println("Start of Loop Recommended Tech: " + recommendedTechStatus.getTechnicianId());
            System.out.println("Start of Loop Recommended Tech Destination: " + techDestination.getTechnicianId());
            
            for(KeyValue keyValue : result.list()) {
    			System.out.println("Qualifier : " + Bytes.toString(keyValue.getQualifier()) + " : Value : " + Bytes.toString(keyValue.getValue()));
    			if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("Status")){
    				currentTechStatus.setStatus(Bytes.toString(keyValue.getValue()));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("Latitude")){
    				currentTechStatus.setLatitude(Double.parseDouble(Bytes.toString(keyValue.getValue())));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("Longitude")){
    				currentTechStatus.setLongitude(Double.parseDouble(Bytes.toString(keyValue.getValue())));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("IpAddress")){
    				currentTechStatus.setIpAddress(Bytes.toString(keyValue.getValue()));
    			} else if(Bytes.toString(keyValue.getQualifier()).equalsIgnoreCase("Port")){
    				currentTechStatus.setPort(Bytes.toString(keyValue.getValue()));
    			}
    		}
    		System.out.println("Post Lookup Loop Recommended Tech: " + recommendedTechStatus.getTechnicianId());
            System.out.println("Post Lookup Loop Recommended Tech Destination: " + techDestination.getTechnicianId());
    		System.out.println("Current Tech Id: " + currentTechStatus.getTechnicianId());
    		System.out.println("Current Tech Status: " + currentTechStatus.getStatus());
    		System.out.println("Current Tech Latitude: " + currentTechStatus.getLatitude());
    		System.out.println("Current Tech Longitude: " + currentTechStatus.getLongitude());
    		System.out.println("Current IpAddress: " + currentTechStatus.getIpAddress());
    		System.out.println("Current Port: " + currentTechStatus.getPort());
    		
    		System.out.println("Inside Loop Recommended Tech: " + recommendedTechStatus.getTechnicianId());
            System.out.println("Inside Loop Recommended Tech Destination: " + techDestination.getTechnicianId());
    		
            currentDistance = distFrom(currentTechStatus.getLatitude(), currentTechStatus.getLongitude(), deviceAlert.getLatitude(), deviceAlert.getLongitude());
            if(currentTechStatus.getStatus().equalsIgnoreCase("Available")){
            	System.out.println("Current Tech is available for repair");
            	if(currentDistance == null || leastDistance == null){
            		System.out.println("Current Tech: " + currentTechStatus.getTechnicianId() + " Current Distance: " + currentDistance + " Least Distance:" + leastDistance);
            		System.out.println("Setting current tech as recommended tech since least distance is null");
            		recommendedTechStatus = copyTechnicianStatus(currentTechStatus);
            		leastDistance = currentDistance;
            	}
            	else if(currentDistance < leastDistance){
            		System.out.println("Current Tech: " + currentTechStatus.getTechnicianId() + " Current Distance: " + currentDistance + " Least Distance:" + leastDistance);
            		System.out.println("Setting current tech as recommended tech since they are currently closest to incident");
            		recommendedTechStatus = copyTechnicianStatus(currentTechStatus);
            		leastDistance = currentDistance;   
            	}
            	else{
            		System.out.println("Current Tech: " + currentTechStatus.getTechnicianId() + " Current Distance: " + currentDistance + " Least Distance:" + leastDistance);
            		System.out.println("Current tech is not recommended as some other tech is closer to the incident");
            	}
            }
            else{
            	System.out.println("Current Tech: " + currentTechStatus.getTechnicianId() + " Current Distance: " + currentDistance + " Least Distance:" + leastDistance);
                System.out.println("Technician " + currentTechStatus.getTechnicianId() + " is already assigned to a repair");
            }
            System.out.println("End of Inside Loop Recommended Tech: " + recommendedTechStatus.getTechnicianId());
            System.out.println("End of Inside Loop Recommended Tech Destination: " + techDestination.getTechnicianId());            
        }
    	
	    System.out.println("Technician " + recommendedTechStatus.getTechnicianId() + " is recommended for this repair");
        
        techDestination.setTechnicianId(recommendedTechStatus.getTechnicianId());
        techDestination.setLatitude(recommendedTechStatus.getLatitude());
        techDestination.setLongitude(recommendedTechStatus.getLongitude());
        techDestination.setDestinationLatitude(deviceAlert.getLatitude());
        techDestination.setDestinationLongitude(deviceAlert.getLongitude());
        techDestination.setStatus(recommendedTechStatus.getStatus());
        techDestination.setIpAddress(recommendedTechStatus.getIpAddress());
        techDestination.setPort(recommendedTechStatus.getPort());
        return techDestination;
    }
    
    public TechnicianStatus copyTechnicianStatus(TechnicianStatus sourceTechStatus){
    	TechnicianStatus targetTechStatus = new TechnicianStatus();
    	targetTechStatus.setTechnicianId(sourceTechStatus.getTechnicianId());
    	targetTechStatus.setIpAddress(sourceTechStatus.getIpAddress());
    	targetTechStatus.setPort(sourceTechStatus.getPort());
    	targetTechStatus.setLatitude(sourceTechStatus.getLatitude());
    	targetTechStatus.setLongitude(sourceTechStatus.getLongitude());
    	targetTechStatus.setStatus(sourceTechStatus.getStatus());
    	return targetTechStatus ;
    }

	public void execute(Tuple tuple) {
		DeviceAlert deviceAlert = (DeviceAlert) tuple.getValueByField("DeviceAlert");
        
        try{
        	TechnicianDestination techDestination = nominateTechnician(deviceAlert);
        	if(techDestination.getTechnicianId() != null && techDestination.getStatus().equalsIgnoreCase("Available")){
        		System.out.println("Emiting Tech Route Request: " + techDestination.getTechnicianId() + " : " + techDestination.toString());
        		collector.emit(tuple, new Values(techDestination));
            }
        	else{
        		System.out.println("Recommended Technician is Null or all Technicians are already assigned");
        		System.out.println("Need additional Techs in the field.....");
        	}
        	collector.ack(tuple);
        }
        catch(IOException e){
        	e.printStackTrace();
        }
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		 this.collector = collector;
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("TechnicianDestination"));
	}
}