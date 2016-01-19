package com.hortonworks.iot.events;

import java.io.Serializable;

public class DeviceStatus implements Serializable {
	private static final long serialVersionUID = 1L;
	private String serialNumber;
	private String status;
	private String state;
	
    public void setSerialNumber(String value){
        serialNumber = value;
    }
    public void setState(String value){
        state = value;
    }
    public void setStatus(String value){
        status = value;
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
}
