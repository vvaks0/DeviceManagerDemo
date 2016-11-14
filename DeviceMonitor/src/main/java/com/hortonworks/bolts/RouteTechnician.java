package com.hortonworks.bolts;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.events.TechnicianDestination;
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

public class RouteTechnician extends BaseRichBolt {
	static final long serialVersionUID = 1L;
    private TechnicianDestination technicianDestination;
    private OutputCollector collector;
    private Constants constants;
    
    public RouteTechnician() {
        super();
    }
    
	public void execute(Tuple tuple) {
		technicianDestination = (TechnicianDestination)tuple.getValueByField("TechnicianDestination");
		try{
        	URL url = new URL("http://" + constants.getNameNodeHost() +":8084/contentListener");
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/json");
            
            System.out.println("To String: " + convertPOJOToJSON(technicianDestination));
            
            OutputStream os = conn.getOutputStream();
    		os.write(convertPOJOToJSON(technicianDestination).getBytes());
    		os.flush();
            
            if (conn.getResponseCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : "
    				+ conn.getResponseCode());
    		}

        } catch (Exception e) {
            e.printStackTrace();
        }
		collector.ack(tuple);
	}

	public void prepare(Map arg0, TopologyContext arg1, OutputCollector collector) {
		this.collector = collector;
		this.constants = new Constants();
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("TechnicianDestination"));
	}
	
	public String convertPOJOToJSON(Object pojo) {
    	String jsonString = "";
    	ObjectMapper mapper = new ObjectMapper();

    	try {
    		jsonString = mapper.writeValueAsString(pojo);
    	} catch (JsonGenerationException e) {
    		e.printStackTrace();
    	} catch (JsonMappingException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return jsonString;
    }
}