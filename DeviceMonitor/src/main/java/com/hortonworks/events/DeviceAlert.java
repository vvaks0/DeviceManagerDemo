package com.hortonworks.events;

import java.io.Serializable;

public class DeviceAlert implements Serializable {
	private static final long serialVersionUID = 1L;
	private String serialNumber;
	private String deviceModel;
    private String alertDescription;
    private Double longitude;
    private Double latitude;
    
    public DeviceAlert() {}
    
    public void setSerialNumber(String value){
        serialNumber = value;
    }
    public void setDeviceModel(String value){
        deviceModel = value;
    }
    public void setAlertDescription(String value){
        alertDescription = value;
    }
    public void setLongitude(Double value){
        longitude = value;
    }
    public void setLatitude(Double value){
        latitude = value;
    }
    
    public String getSerialNumber(){
        return serialNumber;
    }
    public String getDeviceModel(){
        return deviceModel;
    }
    public String getAlertDescription(){
        return alertDescription;
    }
    public Double getLongitude(){
        return longitude;
    }
    public Double getLatitude(){
        return latitude;
    }
}