package com.hortonworks.events;

import java.io.Serializable;

import com.hortonworks.events.DeviceStatus;

public class STBStatus extends DeviceStatus implements Serializable {
	private static final long serialVersionUID = 1L;
	private String deviceModel;
	private Integer internalTemp;
	private Integer signalStrength;
	private Double latitude;
	private Double longitude;
    
	public void setDeviceModel(String value){
        deviceModel = value;
    }
	public void setInternalTemp(Integer value){
        internalTemp = value;
    }
    public void setSignalStrength(Integer value){
        signalStrength = value;
    }
    public void setLatitude(Double value){
        latitude = value;
    }
    public void setLongitude(Double value){
        longitude = value;
    }
    
    public String getDeviceModel(){
        return deviceModel;
    }
    public Integer getInternalTemp(){
        return internalTemp;
    }
    public Integer getSignalStrength(){
        return signalStrength;
    }
    public Double getLatitude(){
        return latitude;
    }
    public Double getLongitude(){
        return longitude;
    }
}