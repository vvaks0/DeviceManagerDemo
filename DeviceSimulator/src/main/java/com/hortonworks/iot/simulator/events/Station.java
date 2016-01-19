package com.hortonworks.iot.simulator.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Station implements Serializable{
	private String stationId;
	private String title;
	private	String shortName;
	private String language;
	private Integer channelNumber;
	private String stationType;
	private boolean isHD;
	private boolean isPayPerView;
	private boolean isVOD;
	private List<ProgramListing> lineup = new ArrayList<ProgramListing>();

	public void setStationId(String value){
		stationId = value;
	}
	public void setTitle(String value){
		title = value;
	}
	public void setShortName(String value){
		shortName = value;
	}
	public void setLanguage(String value){
		language = value;
	}
	public void setStationType(String value){
		stationType = value;
	}
	public void setChannelNumber(Integer value){
		channelNumber = value;
	}
	public void setIsHD(boolean value){
		isHD = value;
	}
	public void setIsPayPerView(boolean value){
		isPayPerView = value;
	}
	public void setIsVOD(boolean value){
		isVOD = value;
	}
	
	public void addProgramListing(ProgramListing value){
		lineup.add(value);
	}
	public String getStationId(){
		return stationId;
	}
	public String getTitle(){
		return title;
	}
	public String getShortName(){
		return shortName;
	}
	public String getLanguage(){
		return language;
	}
	public String getStationType(){
		return stationType;
	}
	public Integer getChannelNumber(){
		return channelNumber;
	}
	public boolean getIsHD(){
		return isHD;
	}
	public boolean getIsPayPerView(){
		return isPayPerView;
	}
	public boolean getIsVOD(){
		return isVOD;
	}
	public List<ProgramListing> getLineup(){
		return lineup;
	}
}