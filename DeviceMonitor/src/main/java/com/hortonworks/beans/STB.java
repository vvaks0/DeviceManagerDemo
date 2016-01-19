package com.hortonworks.beans;

import java.util.Random;

import com.hortonworks.events.STBStatus;
import com.hortonworks.spouts.DeviceSpout;

public class STB implements Runnable {
    private DeviceSpout deviceSpout;
    private String serialNumber;
    private String state; 
    private String status;
    private Integer signalStrength;
    private Integer internalTemp;       //measured in Celsius
    private String externalCommand;
    
    Random random = new Random();
    
    public STB(){
        initialize();    
    }
    
    public STB(DeviceSpout deviceSpout){
        this.deviceSpout = deviceSpout;
        initialize();
    }
    
    public void run() {
 
    	try {
			powerOn();
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
      
    	while(state.equalsIgnoreCase("on")){
    		try {
				generateStatus();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    }
    
    public void initialize(){
        serialNumber = "1000";//deviceSpout.getSerialNumber();
        state = "off";
        status = "normal";
        signalStrength = 85;
        internalTemp = 85;
        externalCommand = "none";
    }
    
    public void sendStatus(){
        STBStatus stbStatus = new STBStatus();
        stbStatus.setSerialNumber(serialNumber);
        stbStatus.setState(state);
        stbStatus.setStatus(status);
        stbStatus.setInternalTemp(internalTemp);
        stbStatus.setSignalStrength(signalStrength);
        
        deviceSpout.enqueueEvent(stbStatus);
    }
    
    public void generateStatus() throws InterruptedException{
    	Integer incident = random.nextInt(6-1) + 1;
    	System.out.println("Incident Randomizer" + incident);
    	if(incident == 5){
    		signalStrength = random.nextInt(70-20) + 20;
        	internalTemp = random.nextInt(110-100) + 100;
    	}
    	else{
    		signalStrength = random.nextInt(95-85) + 85;
        	internalTemp = random.nextInt(95-85) + 85;	
    	}
    	
    	sendStatus();
        Thread.sleep(1000);
    }
    
    public void powerOn() throws InterruptedException {
    	state = "on";
    }
    
    public void powerOff() throws InterruptedException{
        state = "off";
    }
    
    public void setSerialNumber(String value){
        serialNumber = value;
    }
    public void setState(String value){
        state = value;
    }
    public void setStatus(String value){
        status = value;
    }
    public void setInternalTemp(Integer value){
        internalTemp = value;
    }
    public void setSignalStength(Integer value){
        signalStrength = value;
    }
    public void setExternalCommand(String value){
        externalCommand = value;
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
    public Integer getInternalTemp(){
        return internalTemp;
    }
    public Integer getSignalStrength(){
        return signalStrength;
    }
    public String getExternalCommand(){
        return externalCommand;
    }
}
