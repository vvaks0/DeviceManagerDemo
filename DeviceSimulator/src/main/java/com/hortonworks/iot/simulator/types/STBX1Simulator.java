package com.hortonworks.iot.simulator.types;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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
import java.util.zip.GZIPInputStream;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.simulator.events.ChannelTuneEvent;
import com.hortonworks.iot.simulator.events.ProgramListing;
import com.hortonworks.iot.simulator.events.Station;
import com.hortonworks.iot.simulator.events.STBStatus;

public class STBX1Simulator implements Runnable {
    private String serialNumber;
    private String state; 
    private String status;
    private String mode;
    private Integer signalStrength;
    private Integer internalTemp;       //measured in Fahrenheit
    
    private Integer channelTuneEventId;
	private Integer channel;
	private String program;
	private String network;
	private Long eventTimeStamp;
	private Long gmtOffset;
	private Long longTimeStamp;
    
	Map<String,Station> channelMap = new HashMap<String,Station>();
	List<Station> guide = new ArrayList<Station>();
    
    Random random = new Random();
    
    public STBX1Simulator(String deviceSerialNumber, String mode){
        initialize(deviceSerialNumber, mode);    
    }
    
    public void initialize(String deviceSerialNumber, String mode){
        serialNumber = deviceSerialNumber; //deviceSpout.getSerialNumber();
        state = "off";
        channelTuneEventId = 1;
        channelMap = getChannels();
		guide = getListings(channelMap);
		 
		Station station;
		Iterator guideIterator = guide.iterator();
		while(guideIterator.hasNext()){
			station = (Station) guideIterator.next();
			System.out.println("*** Station: "+station.getStationId() + " : " + station.getTitle()+" : "+station.getChannelNumber());
			Iterator listingIterator = station.getLineup().iterator();
			while(listingIterator.hasNext()){
				ProgramListing listing = (ProgramListing) listingIterator.next();
				System.out.println("***************** "+listing.getTitle() + " : " + listing.getStartTimeUTC() + " : " + listing.getEndTimeUTC());
			}
		}
        
        if(mode.equalsIgnoreCase("training")){	
        	this.mode = mode;
        	System.out.print("******************** Training Mode");
        }
        else{
        	this.mode = "simulation";
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
    		Integer audience = random.nextInt(7-1) + 1;
    		try {
    			System.out.println("Cycle Randomizer: " + audience);
    			
    			if(mode.equalsIgnoreCase("training"))
    				runTrainingCycle(audience);
    			else
    				runSimulationCycle(audience);
    			
    		} catch (InterruptedException | ParseException e) {
				e.printStackTrace();
			}
    	}
    }
    public void runSimulationCycle(Integer audience) throws InterruptedException, ParseException{
    	String category = null;
    	long startTime = getCurrentDateEpochStart();
    	switch(audience){
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
    	watchProgram(category, startTime);
    }
    
    public void watchProgram(String demographic, long startTime) throws InterruptedException{
    	String listingPerferredCategory;
    	Station station;
		Iterator guideIterator = guide.iterator();
		
		while(guideIterator.hasNext()){
			station = (Station) guideIterator.next();
			System.out.println("*** Station: "+station.getStationId() + " : " + station.getTitle()+" : "+station.getChannelNumber());
			Iterator listingIterator = station.getLineup().iterator();
			while(listingIterator.hasNext()){
				ProgramListing listing = (ProgramListing) listingIterator.next();
				if(listing.getStartTimeUTC().longValue()==startTime && listing.getCategory().equalsIgnoreCase("Children's"))
				System.out.println("***************** "+listing.getTitle() + " : " +listing.getType() + " : " + listing.getStartTimeUTC() + " : " + listing.getEndTimeUTC());
			}
		}
    	System.out.println("Watching Program: ");
    	sendStatus();
        Thread.sleep(10000);
    }
    
    public void runTrainingCycle(Integer audience) throws InterruptedException{

    }       
    
    public void sendStatus(){
        ChannelTuneEvent channelTuneEvent = new ChannelTuneEvent();
        channelTuneEvent.setDeviceId(serialNumber);
        
        try{
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
    			throw new RuntimeException("Failed : HTTP error code : "
    				+ conn.getResponseCode());
    		}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Integer getRandomExcludes(Integer[] exclude){
    	Integer randomInt = random.nextInt(7-1) + 1;
    	for (int ex : exclude) {
        	if (randomInt < ex) {
            	break;
        	}
        	randomInt++;
    	}
    	return randomInt;
	}
    
    public static long getCurrentDateEpochStart() throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = new Date();
		String str = dateFormat.format(date);
		Date newDate = dateFormat.parse(str);
		long eightHoursMs = 54000000;
		long currentEpochDateEightAM = newDate.getTime() + eightHoursMs;
		
		return currentEpochDateEightAM;
		//System.out.println("Current Epoch Date 0h: " + newDate.getTime()); 
		//System.out.println("Current Epoch Date 8h: " + currentEpochDateEightAM);
		//System.out.println("Current Epoch Date +T: " + System.currentTimeMillis());
		//System.out.println("Formated Date from Epoch: " + format((newDate.getTime() + eightHoursMs)));		
	}
	
	public static long getCurrentDateEpochEnd() throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = new Date();
		String str = dateFormat.format(date);
		Date newDate = dateFormat.parse(str);
		long eightHoursMs = 82800000;
		long currentEpochDateEightAM = newDate.getTime() + eightHoursMs;

		return currentEpochDateEightAM;		
	}
    
	public static Map<String, Station> getChannels(){
		Map<String,Station> channelMap = new HashMap<String,Station>();
		
		try{
        	URL url = new URL("http://xfinitytv.comcast.net/xfinityapi/channel/lineup/headend/3460X/");
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("GET");
    		conn.setRequestProperty("Accept-Encoding", "gzip");
    		conn.setRequestProperty("Content-Type", "application/hal+json;charset=UTF-8"); 
    		
            if (conn.getResponseCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
    		}

            System.out.println("Length : " + conn.getContentLength());

            Reader reader = null;
            if ("gzip".equals(conn.getContentEncoding())) {
                reader = new InputStreamReader(new GZIPInputStream(conn.getInputStream()));
            }else {
            	reader = new InputStreamReader(conn.getInputStream());
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(reader, JsonNode.class); 
            
            //System.out.println(rootNode.toString());
            String field = "";
            String stationField = "";
            String stationValue = "";
            String stationName = "";
            String stationId = "";
            String stationShortName = "";
            String stationLanguage = "";
            String isHD = "";
            String isPayPerView = "";
            String isVOD = "";
            String stationType = "";
            
            for (JsonNode node0 : rootNode.path("_embedded").path("channels")) {
                Station station = new Station();
            	//System.out.println("***Station: "+node0.path("number"));
                station.setChannelNumber(Integer.parseInt(node0.path("number").toString()));
            	for (JsonNode node1 : node0.path("_embedded")) {
	                //System.out.println("******Station Node: "+node1.getElements().next().toString());
	                Iterator stationElements = node1.getElements();
	                Iterator stationFields = node1.getFieldNames();
	                while(stationElements.hasNext()){
	                	stationField = stationFields.next().toString();
	                	stationValue = stationElements.next().toString();
	                	//System.out.println("******Station Node: "+stationField+":"+stationValue);
	                	
	                	if(stationField.equalsIgnoreCase("stationId"))
	                		stationId = stationValue.replace("\"", "");
	                	if(stationField.equalsIgnoreCase("title"))
	                		stationName = stationValue.replace("\"", "");
	                	if(stationField.equalsIgnoreCase("shortName"))
	                		stationShortName = stationValue.replace("\"", "");
	                	if(stationField.equalsIgnoreCase("language"))
	                		stationLanguage = stationValue.replace("\"", "");
	                	if(stationField.equalsIgnoreCase("stationType"))
	                		stationType = stationValue.replace("\"", "");
	                	if(stationField.equalsIgnoreCase("isHD"))
	                		isHD = stationValue;
	                	if(stationField.equalsIgnoreCase("isPayPerView"))
	                		isPayPerView = stationValue;
	                	if(stationField.equalsIgnoreCase("isVOD"))
	                		isVOD = stationValue;
	                }
	                station.setStationId(stationId);
	                station.setTitle(stationName);
	                station.setShortName(stationShortName);
	                station.setLanguage(stationLanguage);
	                station.setStationType(stationType);
	                station.setIsHD(Boolean.parseBoolean(isHD));
	                station.setIsPayPerView(Boolean.parseBoolean(isPayPerView));
	                station.setIsVOD(Boolean.parseBoolean(isVOD));
	                //Thread.sleep(1000);
	                //if(station.getIsHD()==true){
	                
	                //System.out.println(station.getLanguage()+" : "+station.getLanguage().equalsIgnoreCase("ENG") +" : "+ station.getStationType()+""+station.getStationType().equalsIgnoreCase("PRIMARY"));
	                if(station.getLanguage().equalsIgnoreCase("ENG") && 
	                   station.getStationType().equalsIgnoreCase("PRIMARY") && 
	                   station.getIsHD()==true && 
	                   station.getIsPayPerView()==false && 
	                   station.getIsVOD()==false && 
	                   ((station.getChannelNumber()>=300 && station.getChannelNumber()<400) ||
	                	(station.getChannelNumber()>=803 && station.getChannelNumber()<=805) ||
	                	(station.getChannelNumber()>=817 && station.getChannelNumber()<=820) ||
	                	(station.getChannelNumber()>=823 && station.getChannelNumber()<=829) ||
	                	(station.getChannelNumber()>=850 && station.getChannelNumber()<=851) ||
	                	(station.getChannelNumber()>=871 && station.getChannelNumber()<=872) ||
	                	(station.getChannelNumber()>=875 && station.getChannelNumber()<=880) ||
	                   (station.getChannelNumber()>=889 && station.getChannelNumber()<=893))){
	                	System.out.println("******Station Node: "+station.getStationId()+" : "+station.getTitle()+" : "+station.getShortName()+" : "+station.getChannelNumber()+" : "+station.getStationType()+" : "+station.getLanguage()+" : "+station.getIsHD()+" : "+station.getIsPayPerView()+" : "+station.getIsVOD());
	                	channelMap.put(station.getStationId(), station);
	                }
	                /*
	                Iterator stationFields = node1.getFieldNames();
                	while(stationFields.hasNext()){
                		stationField = stationFields.next().toString();
                		System.out.println(stationField);
                		
                		//System.out.println("*** "+stationField+ ": "+node1.path(stationField).size()+":"+node1.path(stationField).toString());      
                		if(node1.path(stationField).getElements().hasNext() && !node1.path(stationField).path(0).path("companyId").isMissingNode()){
                			System.out.println("****** "+field+ ": "+node1.path("stationId").toString()+":"+node1.path(stationField).path(0).path("displayName").toString());
                			if(!channelMap.containsKey(node1.path(stationField).path(0).path("companyId").toString()))	
                				channelMap.put(node1.path("stationId").toString(), node1.path(stationField).path(0).path("displayName").toString());
                		}
                	}    */
                }
            }	            	          	        		
        } catch (Exception e) {
            e.printStackTrace();
        }
		return channelMap; 
	}
	
	public static List<Station> getListings(Map<String,Station> channelMap){
		List<Station> guide = new ArrayList<Station>();
		
		try{
        	//January 1st, 2016, Midnight starttime=1451624400000&endtime=1451638800000
			//http://xfinitytv.comcast.net/xfinityapi/listingcollection/?starttime=1452171600000&endtime=1452186000000&headend=3460X
			//http://xfinitytv.comcast.net/xfinityapi/listingcollection/?starttime=1452171600000&endtime=1452186000000&headend=3460X
			System.out.println("http://xfinitytv.comcast.net/xfinityapi/listingcollection/?starttime="+getCurrentDateEpochStart()+"&endtime="+getCurrentDateEpochEnd()+"&headend=3460X");
			String listingQueryURL = "http://xfinitytv.comcast.net/xfinityapi/listingcollection/?starttime="+getCurrentDateEpochStart()+"&endtime="+getCurrentDateEpochEnd()+"&headend=3460X";
			URL url = new URL(listingQueryURL);
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("GET");
    		conn.setRequestProperty("Accept-Encoding", "gzip");
    		conn.setRequestProperty("Content-Type", "application/hal+json;charset=UTF-8"); 
    		
            if (conn.getResponseCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
    		}

            System.out.println("Length : " + conn.getContentLength());

            Reader reader = null;
            if ("gzip".equals(conn.getContentEncoding())) {
                reader = new InputStreamReader(new GZIPInputStream(conn.getInputStream()));
            }else {
            	reader = new InputStreamReader(conn.getInputStream());
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readValue(reader, JsonNode.class); 
            
            //System.out.println(rootNode.toString());
            String field = "";
            String value = "";
            String listingField = "";
            String listingValue = "";
            Station station = new Station();
            ProgramListing listing = new ProgramListing();
            for (JsonNode node0 : rootNode.path("_embedded").path("stations")) {
            	//System.out.println("***Station: "+node0.path("stationId").toString());
            	System.out.println("***Station: "+channelMap.get(node0.path("stationId").toString())+":"+node0.path("stationId").toString());
            	if(channelMap.containsKey(node0.path("stationId").toString().replace("\"", ""))){
            		station = channelMap.get(node0.path("stationId").toString().replace("\"", ""));
            		
            		for (JsonNode node1 : node0.path("_embedded").path("listings")) {
            			//System.out.println("******Listing: "+node1.path("_type").getTextValue());
            			Iterator listingFields = node1.getFieldNames();
            			while(listingFields.hasNext()){
            				listingField = listingFields.next().toString();
            				listingValue = node1.path(listingField).toString().replace("\"", "");
            				//System.out.println(field);
            				System.out.println("****** "+listingField+ ": "+listingValue);
            				if(listingField.equalsIgnoreCase("startTimeUTC")){
                    			listing.setStartTimeUTC(Long.valueOf(listingValue));
            				}
            				if(listingField.equalsIgnoreCase("endTimeUTC")){
            					listing.setEndTimeUTC(Long.valueOf(listingValue));
            				}
            			}
            			Iterator fields = node1.path("_embedded").path("program").getFieldNames();
            			while(fields.hasNext()){
            				field = fields.next().toString();
            				value = node1.path("_embedded").path("program").path(field).toString();
            				//System.out.println(field);
            				System.out.println("********* "+field+ ": "+value);      
            				//System.out.println("*********Program: "+node1.path("_embedded").path("program").path("title").getTextValue());
            				if(field.equalsIgnoreCase("title")){
            					//listing.setTitle(value);
            				}
            				if(field.equalsIgnoreCase("duration")){
            					listing.setDuration(Integer.parseInt(value));
            				}
            				if(field.equalsIgnoreCase("type")){
            					listing.setType(value);
            					System.out.println("****************************************************************************TYPE: " + value);
            					if(value.replace("\"", "").equalsIgnoreCase("Episode")){
            						System.out.println("****************************************************************************");
            						listing.setTitle(node1.path("_embedded").path("program").path("_embedded").path("parentSeries").path("title").toString());
            					}else{
            						listing.setTitle(node1.path("_embedded").path("program").path("title").toString());
            					}	
            				}
            				if(field.equalsIgnoreCase("category")){
            					listing.setCategory(value);
            				}
            				if(field.equalsIgnoreCase("programLanguage")){
            					listing.setProgramLanguage(value);
            				}
            			}
            			System.out.println(listing.getTitle() + " : " + listing.getStartTimeUTC() + " : " + listing.getEndTimeUTC());
            			station.addProgramListing(listing);
            			Iterator listingIterator = station.getLineup().iterator();
           			 	while(listingIterator.hasNext()){
           			 		ProgramListing listings = (ProgramListing) listingIterator.next();
           			 		System.out.println("***************** "+listings.getTitle() + " : " + listings.getType() + " : " + listings.getStartTimeUTC() + " : " + listings.getEndTimeUTC());
           			 	}
           			 	listing = new ProgramListing();
            		}
            		guide.add(station);
                	station = new Station();
            	}
            }	            	          	        		
        } catch (Exception e) {
            e.printStackTrace();
        }
		
		return guide;
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
    public void setStatus(String value){
        status = value;
    }
    public void setInternalTemp(Integer value){
        internalTemp = value;
    }
    public void setSignalStength(Integer value){
        signalStrength = value;
    }
    
    public String getSerialNumber(){
        return serialNumber;
    }
    public String getState(){
        return state;
    }
    public String getStatus(){
        return status;
    }
    public Integer getInternalTemp(){
        return internalTemp;
    }
    public Integer getSignalStrength(){
        return signalStrength;
    }
}
