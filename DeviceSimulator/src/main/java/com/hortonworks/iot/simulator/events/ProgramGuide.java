package com.hortonworks.iot.simulator.events;

import java.io.InputStreamReader;
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
import java.util.zip.GZIPInputStream;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.simulator.events.ProgramListing;
import com.hortonworks.iot.simulator.events.Station;

public final class ProgramGuide {
	private Integer zipCode;
	private static Map<String,Station> channelMap = new HashMap<String,Station>();
	private static List<Station> guide = new ArrayList<Station>();
	
    private ProgramGuide(){}
    
    public static List<Station> getProgramGuide(Integer zipCode){
    	channelMap = getChannels(zipCode);
		guide = getListings(channelMap, zipCode);
		printProgramGuide();
		
    	return guide;
    }
    
    private static String getHeadEnd(Integer zipCode){
		String headEnd = null;
		
		switch(zipCode){
			case 19101:
				headEnd = "3460X";   //Philadelphia
			break;
			case 33301:
				headEnd = "316017X"; //Ft. Lauderdale
			break;
			case 80123:
				headEnd = "3471X";   //Denver, CO
			break;
			case 94105:
				headEnd = "317006X"; //San Francisco, CA
			break;
			case 97201:
				headEnd = "317093X"; //Portland, OR
			break;
			default:
				headEnd = "3460X";
			break;
		}
		
		return headEnd;
	}
    
    private static long getCurrentDateEpochStart() throws ParseException {
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
	
	private static long getCurrentDateEpochEnd() throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date date = new Date();
		String str = dateFormat.format(date);
		Date newDate = dateFormat.parse(str);
		long eightHoursMs = 86400000; //12AM
		long currentEpochDateEightAM = newDate.getTime() + eightHoursMs;

		return currentEpochDateEightAM;		
	}
    
	private static Map<String, Station> getChannels(Integer zipCode){
		Map<String,Station> channelMap = new HashMap<String,Station>();
		
		try{
        	URL url = new URL("http://xfinitytv.comcast.net/xfinityapi/channel/lineup/headend/"+getHeadEnd(zipCode)+"/");
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
	                   station.getIsVOD()==false &&(
	                   station.getTitle().contains("American Broadcasting Company") ||
	                   station.getTitle().contains("ABC") ||
	                   station.getTitle().contains("National Broadcasting Company") ||
	                   station.getTitle().contains("NBC") ||
	                   station.getTitle().contains("Columbia Broadcasting System") ||
	                   station.getTitle().contains("CBS") ||
	                   station.getTitle().contains("HBO") ||
	                   station.getTitle().contains("Cinemax") ||
	                   station.getTitle().contains("Showtime") ||
	                   station.getTitle().contains("Starz") ||
	                   station.getTitle().contains("Cable News Network") ||
	                   station.getTitle().contains("MSNBC") ||
	                   station.getTitle().contains("Consumer News & Business Channel") ||
	                   station.getTitle().contains("Fox News") ||
	                   station.getTitle().contains("USA Network") ||
	                   station.getTitle().contains("FX") ||
	                   station.getTitle().contains("Turner Network Television") ||
	                   station.getTitle().contains("TBS") ||
	                   station.getTitle().contains("Spike") ||
	                   station.getTitle().contains("Comedy Central") ||
	                   station.getTitle().contains("Syfy") ||
	                   station.getTitle().contains("ESPN") ||
	                   station.getTitle().contains("National Geographic") ||
	                   station.getTitle().contains("Science") ||
	                   station.getTitle().contains("History") ||
	                   station.getTitle().contains("H2") ||
	                   station.getTitle().contains("Disney") ||
	                   station.getTitle().contains("Cartoon Network") ||
	                   station.getTitle().contains("Nickelodeon") ||
	                   station.getTitle().contains("AMC") ||
	                   station.getTitle().contains("Turner Classic") ||
	                   station.getTitle().contains("Encore") ||
	                   station.getTitle().contains("MGM") ||
	                   station.getTitle().contains("IFC")
	                   )
	                   /*((station.getChannelNumber()>=300 && station.getChannelNumber()<400) ||
	                	(station.getChannelNumber()>=803 && station.getChannelNumber()<=805) ||
	                	(station.getChannelNumber()>=817 && station.getChannelNumber()<=820) ||
	                	(station.getChannelNumber()>=823 && station.getChannelNumber()<=829) ||
	                	(station.getChannelNumber()>=850 && station.getChannelNumber()<=851) ||
	                	(station.getChannelNumber()>=871 && station.getChannelNumber()<=872) ||
	                	(station.getChannelNumber()>=875 && station.getChannelNumber()<=880) ||
	                   (station.getChannelNumber()>=889 && station.getChannelNumber()<=893))*/ ){
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
	
	private static List<Station> getListings(Map<String,Station> channelMap, Integer zipCode){
		List<Station> guide = new ArrayList<Station>();
		
		try{
			System.out.println("http://xfinitytv.comcast.net/xfinityapi/listingcollection/?starttime="+getCurrentDateEpochStart()+"&endtime="+getCurrentDateEpochEnd()+"&headend="+getHeadEnd(zipCode));
			String listingQueryURL = "http://xfinitytv.comcast.net/xfinityapi/listingcollection/?starttime="+getCurrentDateEpochStart()+"&endtime="+getCurrentDateEpochEnd()+"&headend="+getHeadEnd(zipCode);
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
            				value = node1.path("_embedded").path("program").path(field).toString().replace("\"", "");
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
            					if(value.replace("\"", "").equalsIgnoreCase("Episode")){
            						listing.setTitle(node1.path("_embedded").path("program").path("_embedded").path("parentSeries").path("title").toString().replace("\"", ""));
            					}else{
            						listing.setTitle(node1.path("_embedded").path("program").path("title").toString().replace("\"", ""));
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
	
	private static void printProgramGuide(){
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
		}
	}
}