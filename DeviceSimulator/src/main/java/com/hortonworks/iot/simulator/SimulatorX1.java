package com.hortonworks.iot.simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SimulatorX1 {

	public static void main(String[] args) {
		 try{
	        	URL url = new URL("http://xfinitytv.comcast.net/xfinityapi/station/5819410770390906117");
	    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    		conn.setDoOutput(true);
	    		conn.setRequestMethod("GET");
	    		conn.setRequestProperty("Accept", "application/json");
	            
	            if (conn.getResponseCode() != 200) {
	    			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
	    		}

	            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

	        	String output;
	        	System.out.println("Output from Server .... \n");
	        	while ((output = br.readLine()) != null) {
	        		System.out.println(output);
	        	}
	        		
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	}

}
