package com.hortonworks.iot.simulator.types;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.simulator.events.ChannelTuneEvent;
import com.hortonworks.iot.simulator.events.ProgramGuide;
import com.hortonworks.iot.simulator.events.ProgramListing;
import com.hortonworks.iot.simulator.events.Station;

public class X1TunerSimulator implements Runnable {
	private String serialNumber;
	private String state; 
	private String mode;
	    
	private Integer channelTuneEventId;
	private Integer channel;
	private String program;
	private String network;
	private Long eventTimeStamp;
	private Long gmtOffset;
	private Long longTimeStamp;
	private String market;
	private Integer zipCode;
		
	private List<Station> guide = new ArrayList<Station>();
	private List<Integer> excludes = new ArrayList<Integer>();
	    
	static Random random = new Random();
	
	public X1TunerSimulator(List<Station> guide, String deviceSerialNumber, String mode, String market, Integer zipCode){
		initialize(guide, deviceSerialNumber, mode, market, zipCode);
	}
	
	public void initialize(List<Station> guide, String deviceSerialNumber, String mode, String market, Integer zipCode){
        this.serialNumber = deviceSerialNumber;
        this.market = market;
        this.mode = mode;
        this.zipCode = zipCode;
        this.state = "off";
        this.channelTuneEventId = 1;
        this.guide = guide;
        /*
		Station station;
		Iterator guideIterator = guide.iterator();
		while(guideIterator.hasNext()){
			station = (Station) guideIterator.next();
			System.out.println("****** Station: "+station.getStationId() + " : " + station.getTitle()+" : "+station.getChannelNumber());
			Iterator listingIterator = station.getLineup().iterator();
			while(listingIterator.hasNext()){
				ProgramListing listing = (ProgramListing) listingIterator.next();
				System.out.println("***************** "+listing.getTitle() + " : "+listing.getCategory() + " : " + listing.getStartTimeUTC() + " : " + listing.getEndTimeUTC());
			}
		}*/
        
        if(mode.equalsIgnoreCase("training")){	
        	mode = "training";
        	System.out.print("******************** Training Mode");
        }
        else{
        	mode = "simulation";
        	System.out.print("******************** Simulation Mode");
        }	
    }

	public void run() {
    	try {
			powerOn();
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
      
    	while(state.equalsIgnoreCase("on")){
    		if(mode.equalsIgnoreCase("training"))
				try {
					runTrainingCycle();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			else
				try {
					runSimulationCycle();
				} catch (InterruptedException | ParseException | IOException e) {
					e.printStackTrace();
				}
    	}
	}
	
	public void runSimulationCycle() throws InterruptedException, ParseException, IOException{
    	long startTime = getCurrentDateEpochStart();
    	watchProgram(startTime);
    	powerOff();
    }
    
    public Integer watchProgram(long startTime) throws InterruptedException, IOException{
    	int currentListingKey = 1; 
    	Station station;
		Iterator<Station> guideIterator = guide.iterator();
		Map<Integer, ChannelTuneEvent> potentialChannelTuneEvents = new HashMap<Integer, ChannelTuneEvent>();
		String preferredCategory = selectPreferredCategory(excludes);
		while(guideIterator.hasNext()){			
			station = (Station) guideIterator.next();
			//System.out.println("*** Station: "+station.getStationId() + " : " + station.getTitle()+" : "+station.getChannelNumber());
			Iterator listingIterator = station.getLineup().iterator();
			while(listingIterator.hasNext()){
				ProgramListing listing = (ProgramListing) listingIterator.next();
				//System.out.println("***************** "+startTime+" : "+preferredCategory);
				if(listing.getStartTimeUTC().longValue()==startTime && listing.getCategory().equalsIgnoreCase(preferredCategory)){
					//System.out.println("***************** "+listing.getTitle() + " : " +listing.getType() + " : " + listing.getStartTimeUTC() + " : " + listing.getEndTimeUTC());
					ChannelTuneEvent channelTuneEvent = new ChannelTuneEvent();
					channelTuneEvent.setId(serialNumber+""+channelTuneEventId);
					channelTuneEvent.setDeviceId(serialNumber);
					channelTuneEvent.setProgram(listing.getTitle());
					channelTuneEvent.setGmtOffset((long) 0000);
					channelTuneEvent.setChannel(station.getChannelNumber());
					channelTuneEvent.setNetwork(station.getTitle());
					channelTuneEvent.setMarket(market);
					channelTuneEvent.setEventTimeStamp(startTime);
					channelTuneEvent.setLongTimeStamp(listing.getEndTimeUTC());
					channelTuneEvent.setZipCode(zipCode);
					potentialChannelTuneEvents.put(currentListingKey, channelTuneEvent);
					currentListingKey++;
				}
			}
		}
		System.out.println("***************** size"+potentialChannelTuneEvents.size());
		if(potentialChannelTuneEvents.size() > 0){
			Integer potentialChannelTuneEventsSize = potentialChannelTuneEvents.size() + 1;
			Integer selectedProgramKey = (random.nextInt(potentialChannelTuneEventsSize-1) + 1);
			System.out.println("***************** key"+selectedProgramKey);
			ChannelTuneEvent selectedChannelTuneEvent = potentialChannelTuneEvents.get(selectedProgramKey);
			System.out.println("Watching Program: " + selectedChannelTuneEvent.getProgram());
			sendStatus(selectedChannelTuneEvent);
			channelTuneEventId++;
			excludes.clear();
			Thread.sleep(1000);
			watchProgram(selectedChannelTuneEvent.getLongTimeStamp());
		}
		else if(excludes.size() < 6){
			int excludedCategory = 0;
			switch(preferredCategory){
	    	case "Movie":
	    		excludedCategory = 1;
	    		break;
	    	case "Sports":
	    		excludedCategory = 2;
	    		break;
	    	case "Children's":
	    		excludedCategory = 3;
	    		break;
	    	case "Other":
	    		excludedCategory = 4;
	    		break;
	    	case "News":
	    		excludedCategory = 5;
	    		break;
	    	case "Lifestyle":
	    		excludedCategory = 6;
	    		break;	
	    	}
			excludes.add(excludedCategory);
			watchProgram(startTime);
		}
			
		return 0;
    }
    
    public void runTrainingCycle() throws InterruptedException{}       
    
    public String selectPreferredCategory(List<Integer> excludes){
    	String category = null;
    	Integer preference = getRandomExcludes(excludes);
    	System.out.println("***************** Selecting Program Category: "+ preference);
    	switch(preference){
    	case 1:
    		category = "Movie";
    		break;
    	case 2:
    		category = "Sports";
    		break;
    	case 3:
    		category = "Children's";
    		break;
    	case 4:
    		category = "Other";
    		break;
    	case 5:
    		category = "News";
    		break;
    	case 6:
    		category = "Lifestyle";
    		break;	
    	}
    	
    	return category;
    }
    
    public Integer getRandomExcludes(List<Integer> excludes){
    	Integer maxInt = 7;
    	Integer randomInt = random.nextInt(maxInt-1) + 1;
    	for (int ex : excludes) {
        	if (randomInt < ex) {
            	break;
        	}
        	randomInt++;
        	if(randomInt == maxInt)
        		randomInt = 1;
    	}
    	return randomInt;
	}
    
    public void sendStatus(ChannelTuneEvent channelTuneEvent) throws IOException{
        URL url = new URL("http://localhost:8082/contentListener");
    	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	conn.setDoOutput(true);
    	conn.setRequestMethod("POST");
    	conn.setRequestProperty("Content-Type", "application/json");
            
        System.out.println("To String: " + convertPOJOToJSON(channelTuneEvent));
            
        OutputStream os = conn.getOutputStream();
    	os.write(convertPOJOToJSON(channelTuneEvent).getBytes());
    	os.flush();
            
        if (conn.getResponseCode() != 200) {
    		throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
    	}
    }
    
    public long getCurrentDateEpochStart() throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = new Date();
		String str = dateFormat.format(date);
		Date newDate = dateFormat.parse(str);
		long eightHoursMs = 72000000;//54000000; //3PM
		long currentEpochDateEightAM = newDate.getTime() + eightHoursMs;
		
		return currentEpochDateEightAM;
		//System.out.println("Current Epoch Date 0h: " + newDate.getTime()); 
		//System.out.println("Current Epoch Date 8h: " + currentEpochDateEightAM);
		//System.out.println("Current Epoch Date +T: " + System.currentTimeMillis());
		//System.out.println("Formated Date from Epoch: " + format((newDate.getTime() + eightHoursMs)));		
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
    
    public void powerOn() throws InterruptedException {
    	state = "on";
    }
    
    public void powerOff() throws InterruptedException{
        state = "off";
    }
    
    public void setSerialNumber(String value){
        serialNumber = value;
    }
    public void setState(String value){
        state = value;
    }
    
    public String getSerialNumber(){
        return serialNumber;
    }
    public String getState(){
        return state;
    }
}