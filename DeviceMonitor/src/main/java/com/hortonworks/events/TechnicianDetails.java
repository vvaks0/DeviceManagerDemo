package com.hortonworks.events;

import java.io.Serializable;

//import java.util.HashMap;
//import java.util.Map;

public class TechnicianDetails extends TechnicianStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer yearsExperience;
    private Integer rating;
    //private Map<Integer, String> parts = new HashMap<Integer, String>();
    public TechnicianDetails() {
        super();
    }
    
    public Integer getYearsExperience(){
        return yearsExperience;
    }
    public Integer getRating(){
        return rating;
    }
    public void setYearsExperience(Integer value){
        yearsExperience = value;
    }
    public void setRating(Integer value){
        rating = value;
    }
}