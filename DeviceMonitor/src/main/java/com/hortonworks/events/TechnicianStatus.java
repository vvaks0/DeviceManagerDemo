package com.hortonworks.events;

import java.io.Serializable;

public class TechnicianStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    private String technicianId;
    private Double longitude;
    private Double latitude;
    private String status;
    private String ipaddress;
    private String port;
    private String eventType;

    public TechnicianStatus() {}
    
    public void setTechnicianId(String value){
        technicianId = value;   
    }
    public void setLongitude(Double value){
        longitude = value;
    }
    public void setLatitude(Double value){
        latitude = value;
    }
    public void setStatus(String value){
        status = value;
    }
    public void setIpAddress(String value){
        ipaddress = value;
    }
    public void setPort(String value){
        port = value;
    }
    public void setEventType(String value){
        eventType = value;
    }
    public String getTechnicianId(){
        return technicianId;
    }
    public Double getLongitude(){
        return longitude;
    }
    public Double getLatitude(){
        return latitude;
    }
    public String getStatus(){
        return status;
    }
    public String getIpAddress(){
        return ipaddress;
    }
    public String getPort(){
        return port;
    }
    public String getEventType(){
        return eventType;
    }
}