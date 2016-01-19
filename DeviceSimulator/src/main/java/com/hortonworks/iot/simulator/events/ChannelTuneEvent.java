package com.hortonworks.iot.simulator.events;

import java.io.Serializable;

public class ChannelTuneEvent implements Serializable {
	private String id;
	private String deviceId;
	private Integer channel;
	private String program;
	private String network;
	private Long eventTimeStamp;
	private Long gmtOffset;
	private Long longTimeStamp;
	private String market;
	private Integer zipCode;
	
	public void setId(String value){
		id = value;
	}
	public void setDeviceId(String value){
		deviceId = value;
	}
	public void setProgram(String value){
		program = value;
	}
	public void setNetwork(String value){
		network = value;
	}
	public void setChannel(Integer value){
		channel = value;
	}
	public void setEventTimeStamp(Long value){
		eventTimeStamp = value;
	}
	public void setLongTimeStamp(Long value){
		longTimeStamp = value;
	}
	public void setGmtOffset(Long value){
		gmtOffset = value;
	}
	public void setMarket(String value){
		market = value;
	}
	public void setZipCode(Integer value){
		zipCode = value;
	}
	public String getId(){
		return id;
	}
	public String getDeviceId(){
		return deviceId;
	}
	public String getProgram(){
		return program;
	}
	public String getNetwork(){
		return network;
	}
	public Integer getChannel(){
		return channel;
	}
	public Long getEventTimeStamp(){
		return eventTimeStamp;
	}
	public Long getLongTimeStamp(){
		return longTimeStamp;
	}
	public Long getGmtOffset(){
		return gmtOffset;
	}
	public String getMarket(){
		return market;
	}
	public Integer getZipCode(){
		return zipCode;
	}
}