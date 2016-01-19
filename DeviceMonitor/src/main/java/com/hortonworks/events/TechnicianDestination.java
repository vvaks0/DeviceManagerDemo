package com.hortonworks.events;

public class TechnicianDestination extends TechnicianStatus {
	private static final long serialVersionUID = 1L;
	private Double destinationLatitude;
    private Double destinationLongitude;
    
    public TechnicianDestination() {
        super();
    }
    
    public void setDestinationLongitude(Double value){
        destinationLongitude = value;
    }
    public void setDestinationLatitude(Double value){
        destinationLatitude = value;
    }

    public Double getDestinationLongitude(){
        return destinationLongitude;
    }
    public Double getDestinationLatitude(){
        return destinationLatitude;
    }
}