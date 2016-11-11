package com.hortonworks.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.storm.kafka.KeyValueScheme;
import org.apache.storm.kafka.StringScheme;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.events.STBStatus;

/*
import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import storm.kafka.KeyValueScheme;
*/

/**
 * Encodes a byte array into a single UTF-8 string. Very useful for testing and passing raw JSON messages around without
 * proper deserialization.
 */
public class DeviceEventJSONScheme implements KeyValueScheme {
	private static final long serialVersionUID = 1L;
	private static final Charset UTF8 = Charset.forName("UTF-8");

    public List<Object> deserialize(byte[] bytes) {
        String eventJSONString = new String(bytes, UTF8);
        STBStatus stbStatus = null;
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			stbStatus = mapper.readValue(eventJSONString, STBStatus.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new Values(stbStatus);
    }

	public List<Object> deserializeKeyAndValue(byte[] key, byte[] value) {
		String eventJSONString = new String(value, UTF8);
        STBStatus stbStatus = null;
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			stbStatus = mapper.readValue(eventJSONString, STBStatus.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new Values(stbStatus);
	}

	public List<Object> deserializeKeyAndValue(ByteBuffer key, ByteBuffer value) {
		String eventKey = StringScheme.deserializeString(key);
		String eventJSONString = StringScheme.deserializeString(value);
		STBStatus stbStatus = null;
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			stbStatus = mapper.readValue(eventJSONString, STBStatus.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new Values(stbStatus);
	}
	public List<Object> deserialize(ByteBuffer value) {
		String eventJSONString = StringScheme.deserializeString(value);
		STBStatus stbStatus = null;
        ObjectMapper mapper = new ObjectMapper();
        
        try {
			stbStatus = mapper.readValue(eventJSONString, STBStatus.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return new Values(stbStatus);
	}
	
	public Fields getOutputFields() {
        return new Fields("DeviceStatus");
    }
}