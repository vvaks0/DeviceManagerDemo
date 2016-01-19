package com.hortonworks.iot.simulator.events;

import java.io.Serializable;

public class ProgramListing implements Serializable{
	private String title;
	private Integer duration;
	private String type;
	private String category;
	private String programLanguage;
	private Long startTimeUTC;
	private Long endTimeUTC;

	public void setTitle(String value){
        title = value;
    }
	public void setDuration(Integer value){
        duration = value;
    }
    public void setType(String value){
        type = value;
    }
    public void setCategory(String value){
        category = value;
    }
    public void setProgramLanguage(String value){
        programLanguage = value;
    }
    public void setStartTimeUTC(Long value){
    	startTimeUTC = value;
    }
    public void setEndTimeUTC(Long value){
    	endTimeUTC = value;
    }
    
    public String getTitle(){
        return title;
    }
    public Integer duration(){
        return duration;
    }
    public String getType(){
        return type;
    }
    public String getCategory(){
        return category;
    }
    public String getProgramLanguage(){
        return programLanguage;
    }
    public Long getStartTimeUTC(){
        return startTimeUTC;
    }
    public Long getEndTimeUTC(){
        return endTimeUTC;
    }
}