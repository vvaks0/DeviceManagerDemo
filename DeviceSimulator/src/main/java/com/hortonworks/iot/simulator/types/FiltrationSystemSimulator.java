package com.hortonworks.iot.simulator.types;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.simulator.events.FiltrationStatus;

public class FiltrationSystemSimulator implements Runnable {
    private String serialNumber;
    private String state; 
    private String status;
    private String mode;
    private Integer internalPressure;
    private Integer flowRate;
    private Integer flowTemp;
    private Integer batchNumber;
    private Integer hoursFromStart;
    
    private String externalCommand;
    private Integer cyclesCompleted = 0;
    
    Random random = new Random();
    
    public FiltrationSystemSimulator(String deviceSerialNumber, String mode){
        initialize(deviceSerialNumber, mode);    
    }
    
    public void run() {
 
    	try {
			powerOn();
			normalCycle();
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
      
    	while(state.equalsIgnoreCase("on")){
    		Integer incident = random.nextInt(6-1) + 1;
    		try {
    			System.out.println("Cycle Randomizer: " + incident);
    			
    			if(mode.equalsIgnoreCase("training"))
    				runTrainingCycle(incident);
    			else
    				runSimulationCycle(incident);
    			
    		} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    }
    public void runSimulationCycle(Integer incident) throws InterruptedException{
    	if(incident > 3 && cyclesCompleted >= 1 ){
			pressureFailCycle();
			//powerOff();
		}
		else{
			normalCycle();
			cyclesCompleted++;
		}
    }
    
    public void runTrainingCycle(Integer incident) throws InterruptedException{
		if(incident > 3){
			pressureFailCycle();
			//cyclesCompleted++;
		}
		else{
			normalCycle();
			//cyclesCompleted++;
		}
    }
    
    public void initialize(String deviceSerialNumber, String mode){
        serialNumber = deviceSerialNumber; //deviceSpout.getSerialNumber();
        state = "off";
        status = "normal";
        setInternalPressure(20);
        setFlowTemp(37);
        setFlowRate(50);
        batchNumber = 0;
        externalCommand = "none";
        if(mode.equalsIgnoreCase("training")){	
        	this.mode = mode;
        	System.out.print("******************** Training Mode");
        }
        else{
        	this.mode = "simulation";
        	System.out.print("******************** Simulation Mode");
        }	
    }
    
    public void sendStatus(){
    	FiltrationStatus filtrationStatus = new FiltrationStatus();
    	filtrationStatus.setSerialNumber(serialNumber);
    	filtrationStatus.setState(state);
    	filtrationStatus.setStatus(status);
    	filtrationStatus.setFlowRate(flowRate);
    	filtrationStatus.setFlowTemp(flowTemp);
    	filtrationStatus.setInternalPressure(internalPressure);

        try{
        	URL url = new URL("http://localhost:8082/contentListener");
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/json");
            
            System.out.println("To String: " + convertPOJOToJSON(filtrationStatus));
            
            OutputStream os = conn.getOutputStream();
    		os.write(convertPOJOToJSON(filtrationStatus).getBytes());
    		os.flush();
            
            if (conn.getResponseCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : "
    				+ conn.getResponseCode());
    		}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void normalCycle() throws InterruptedException{
    	System.out.println("Starting New Normal Cycle");
    	for(int i=0; i<15; i++){
    		internalPressure = random.nextInt(55-40) + 40;
    		flowRate = random.nextInt(55-45) + 45;
			flowTemp = random.nextInt(39-36) + 36;
			hoursFromStart +=12;
        	sendStatus();
            Thread.sleep(1000);
    	}    	  
    }
    
    public void pressureFailCycle() throws InterruptedException{
    	System.out.println("Starting New PressureFail Cycle");
    	for(int i=0; i<15; i++){
    		if(i==0){
    			flowRate = random.nextInt(55-45) + 45;
    			flowTemp = random.nextInt(39-36) + 36;
    			internalPressure = 40;
    		}
    		else{
    			flowRate = random.nextInt(55-45) + 45;
    			flowTemp = random.nextInt(39-36) + 36;
    			internalPressure = internalPressure + random.nextInt(4-1) + 1;
    			hoursFromStart +=12;
    		}
    		
    		sendStatus();
            Thread.sleep(1000);
    	}
    }
    
    /*
    public void generateStatus() throws InterruptedException{
    	Integer incident = random.nextInt(21-1) + 1;
    	System.out.println("Incident Randomizer: " + incident);
    	if(incident == 20 && eventsSent >= 30){
    		signalStrength = random.nextInt(70-20) + 20;
        	internalTemp = random.nextInt(110-100) + 100;
    	}
    	else{
    		signalStrength = random.nextInt(95-85) + 85;
        	internalTemp = random.nextInt(95-85) + 85;	
    	}
    	
    	sendStatus();
    	eventsSent++;
        Thread.sleep(1000);
    }
    */
    
    public String convertPOJOToJSON(Object pojo) {
    	String jsonString = "";
    	ObjectMapper mapper = new ObjectMapper();

    	try {
    		jsonString = mapper.writeValueAsString(pojo);
    	} catch (JsonGenerationException e) {
    		e.printStackTrace();
    	} catch (JsonMappingException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return jsonString;
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
    public String getExternalCommand(){
        return externalCommand;
    }

	public Integer getInternalPressure() {
		return internalPressure;
	}

	public void setInternalPressure(Integer internalPressure) {
		this.internalPressure = internalPressure;
	}

	public Integer getFlowRate() {
		return flowRate;
	}

	public void setFlowRate(Integer flowRate) {
		this.flowRate = flowRate;
	}

	public Integer getFlowTemp() {
		return flowTemp;
	}

	public void setFlowTemp(Integer flowTemp) {
		this.flowTemp = flowTemp;
	}

	public Integer getBatchNumber() {
		return batchNumber;
	}

	public void setBatchNumber(Integer batchNumber) {
		this.batchNumber = batchNumber;
	}

	public Integer getHoursFromStart() {
		return hoursFromStart;
	}

	public void setHoursFromStart(Integer hoursFromStart) {
		this.hoursFromStart = hoursFromStart;
	}
}