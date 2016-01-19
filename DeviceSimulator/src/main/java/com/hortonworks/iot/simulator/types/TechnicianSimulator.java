package com.hortonworks.iot.simulator.types;

import com.google.appengine.api.search.GeoPoint;
import com.hortonworks.iot.simulator.events.TechnicianDestination;
import com.hortonworks.iot.simulator.events.TechnicianStatus;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

public class TechnicianSimulator implements Runnable {
    private String technicianId;
    private String ipaddress;
    private String port;
    private String origin;
    private String destination;
    private String waypointString;
    private String status;
    private boolean assigned = false;
    private GeoPoint currentLocation;
    private List<List<GeoPoint>> tripList = new ArrayList<List<GeoPoint>>();
    private List<GeoPoint> currentTrip = new ArrayList<GeoPoint>();
    private List<TechnicianDestination> techDestinationList = new ArrayList<TechnicianDestination>();
    private String externalCommand;
    
    public TechnicianSimulator(String technicianId, String ipaddress, String port) {
        initialize(technicianId, ipaddress, port);
    }
    
    public void initialize(String technicianId, String ipaddress, String port){
        this.technicianId = technicianId;
        this.ipaddress = ipaddress;
        this.port = port;
        externalCommand = "none";
        status = "Available";
        
        switch(technicianId){
        	case "1000":
        		waypointString = "19122|19121|19146|19147";
        		origin = "19123";
        		destination = "19123";
        	break;
        	case "2000":
        		waypointString = "19146|19147|19122|19121";
        		origin = "19103";
        		destination = "19103";
        	break;
        	case "3000":
        		waypointString = "19122|19121|19130|19146";
        		origin = "19147";
        		destination = "19147";
        	break;
        	
        }
        tripList.add(getDirectionsWayPoints(origin, destination, waypointString));
        System.out.println("***** " + origin + " , " + destination + " , " + tripList.get(0));
    }

    public void run() {
    	CacheManager cacheManager = CacheManager.getInstance();
		Cache cache = cacheManager.getCache("TechnicianRouteRequest");
		TechnicianDestination newTechnicianDestination = null;
		
        Iterator<GeoPoint> currentLocationIterator;
        for(int i=0; i<tripList.size(); i++) {
            currentTrip = tripList.get(i);
            currentLocationIterator = currentTrip.iterator();
            System.out.println(currentLocationIterator.hasNext());
            while(currentLocationIterator.hasNext()){
            	if(cache.get(technicianId) != null){
            		newTechnicianDestination = (TechnicianDestination)((Element) cache.get(technicianId)).getObjectValue();
            		System.out.println("Recieved Request for Action :" + newTechnicianDestination);
            		addTechnicianDestination(newTechnicianDestination);
            	}
            	//If the Tech Route Request Cache conatins a Tech Destination
                if(techDestinationList.size() > 0){
                    status = "Assigned";
                	//create new tech destination from object in cache
                    TechnicianDestination techDestination = techDestinationList.get(0);
                    //get directions to destination and add to trip list
                    tripList.add(getDirectionsWayPoints(currentLocation.getLatitude()+","+currentLocation.getLongitude(), techDestination.getDestinationLatitude()+","+techDestination.getDestinationLongitude(), ""));
                    //move trip list from current trip the new trip
                    i++;
                    currentTrip = tripList.get(i);
                    //reset current location from current location to the first location in the new trip
                    currentLocationIterator = currentTrip.iterator();
                    currentLocation = currentLocationIterator.next();
                    //remove route request from cache
                    techDestinationList.remove(0);
                    cache.remove(technicianId);
                    sendTechnicianStatus();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }else{
                    currentLocation = currentLocationIterator.next();
                    sendTechnicianStatus();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }   
                }
            }
        }
    }
    public void sendTechnicianStatus(){
        System.out.println("Sending Technician Status ************************");
        TechnicianStatus technicianStatus = new TechnicianStatus();
        technicianStatus.setTechnicianId(this.technicianId);
        technicianStatus.setLatitude(this.currentLocation.getLatitude());
        technicianStatus.setLongitude(this.currentLocation.getLongitude());
        technicianStatus.setEventType("Technician");
        technicianStatus.setStatus(status);
        technicianStatus.setIpAddress(ipaddress);
        technicianStatus.setPort(port);
        //technicianSpout.enqueueEvent(technicianStatus);
        
        try{
        	URL url = new URL("http://localhost:8082/contentListener");
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/json");
            
            System.out.println("To String: " + convertPOJOToJSON(technicianStatus));
            
            OutputStream os = conn.getOutputStream();
    		os.write(convertPOJOToJSON(technicianStatus).getBytes());
    		os.flush();
            
            if (conn.getResponseCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : "
    				+ conn.getResponseCode());
    		}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void addTechnicianDestination(TechnicianDestination technicianDestination){
        techDestinationList.add(technicianDestination);
    }
    public void setTechnicianId(String value){
        technicianId = value;
    }
    public void setStatus(String value){
        status = value;
    }
    public void setOrigin(String value){
        origin = value;
    }
    public void setDestination(String value){
        destination = value;
    }
    public void setExternalCommand(String value){
        externalCommand = value;
    }
    public void setAssigned(boolean value){
        assigned = value;
    }
    public String getStatus(){
        return status;
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
    public String getExternalCommand(){
        return externalCommand;
    }
    public boolean getAssigned(){
        return assigned;
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
    
    public static List<GeoPoint> getDirectionsWayPoints(String origin, String destination, String waypoints){
        URL url;
        List<GeoPoint> polyLine = new ArrayList<GeoPoint>();
        
        try {
            if(waypoints == ""){
                url = new URL("https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination);
            }
            else{    
                url = new URL("https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination +"&waypoints=" + waypoints);
            }
            System.out.println(url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }       
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));                 
                            
            JsonFactory f = new MappingJsonFactory();
            JsonParser jp = f.createJsonParser(br);
            JsonToken current;
            current = jp.nextToken();
            if (current != JsonToken.START_OBJECT) {
                System.out.println("Error: root should be object: quiting.");
            }

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jp.getCurrentName();
                System.out.println(fieldName);
                // move from field name to field value
                current = jp.nextToken();
                if (fieldName.equals("routes")) {
                    if (current == JsonToken.START_ARRAY) {
                    // For each of the records in the array
                        while (jp.nextToken() != JsonToken.END_ARRAY) {
                            // read the record into a tree model,
                            // this moves the parsing position to the end of it
                            JsonNode node = jp.readValueAsTree();
                            System.out.println("Node: " + node);
                            // And now we have random access to everything in the object
                            System.out.println("field1: " + node.get("overview_polyline").get("points").toString());
                            polyLine = decodePolyLineString(node.get("overview_polyline").get("points").getTextValue());     
                            System.out.println(polyLine.toString());
                            //System.out.println("field2: " + node.get("").getValueAsText());
                        }
                    } else {
                        System.out.println("Error: records should be an array: skipping.");
                        jp.skipChildren();
                    }
                } else {
                    //System.out.println("Unprocessed property: " + fieldName);
                    jp.skipChildren();
                }
            }
            conn.disconnect();              
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
             e.printStackTrace();
        }
            return polyLine;
    }
            
    public static void printNode(JsonNode node) {
        Iterator<String> fieldNames = node.getFieldNames();
        while(fieldNames.hasNext()){
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);
            if (fieldValue.isObject()) {
                System.out.println(fieldName + " :");
                printNode(fieldValue);
            } else {
                String value = fieldValue.asText();
                System.out.println(fieldName + " : " + value);
            }
        }
    }
            
    public static List<GeoPoint> decodePolyLineString(String encoded){
        List<GeoPoint> decodedPath = new ArrayList<GeoPoint>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            GeoPoint p = new GeoPoint((double) (((double) lat / 1E5)),
            (double) (((double) lng / 1E5)));
            decodedPath.add(p);
        }
        return decodedPath;
    }
}