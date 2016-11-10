package com.hortonworks.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.events.TechnicianStatus;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import storm.kafka.KeyValueScheme;

public class TechnicianEventJSONScheme implements KeyValueScheme {

	private static final long serialVersionUID = 1L;
	private static final Charset UTF8 = Charset.forName("UTF-8");

    public List<Object> deserialize(byte[] bytes) {
        String eventJSONString = new String(bytes, UTF8);
        TechnicianStatus technicianStatus = null;
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			technicianStatus = mapper.readValue(eventJSONString, TechnicianStatus.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new Values(technicianStatus);
    }

	public List<Object> deserializeKeyAndValue(byte[] key, byte[] value) {
		String eventJSONString = new String(value, UTF8);
        TechnicianStatus technicianStatus = null;
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			technicianStatus = mapper.readValue(eventJSONString, TechnicianStatus.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new Values(technicianStatus);
	}
	
	public List<Object> deserializeKeyAndValue(ByteBuffer arg0, ByteBuffer arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public List<Object> deserialize(ByteBuffer arg0) {
		// TODO Auto-generated method stub
		return null;
	}
    public Fields getOutputFields() {
        return new Fields("TechnicianStatus");
    }
}