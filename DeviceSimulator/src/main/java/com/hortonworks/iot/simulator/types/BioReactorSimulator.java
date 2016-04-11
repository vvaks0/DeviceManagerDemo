package com.hortonworks.iot.simulator.types;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.hortonworks.iot.simulator.events.BioReactorStatus;
import com.hortonworks.iot.simulator.events.STBStatus;

public class BioReactorSimulator  implements Runnable {
    private String serialNumber;
    private String state; 
    private String status;
    private String mode;
    private Integer volume;
    private double glucoseLevel;
    private double lactateLevel;
    private double mediaLevel;
    private double phLevel;
    private double disolvedOxygen;
    private double internalTemp;
    private double cellsPerML;
    private Integer batchNumber;
    private Integer hoursFromStart;
    private double initialCellConcentration;
    private boolean goldenBatch;
    private List<String> transactionList = new ArrayList<String>();
    
    private String externalCommand;
    private Integer cyclesCompleted = 0;
    
    Random random = new Random();
    
    public BioReactorSimulator(String deviceSerialNumber, String mode){
        initialize(deviceSerialNumber, mode);    
    }
    
    public void initialize(String deviceSerialNumber, String mode){
    	serialNumber = deviceSerialNumber; //deviceSpout.getSerialNumber();
        volume = 200000; //Measured in MiliLiters
        initialCellConcentration = 300000;
        state = "off";
        status = "normal";
        batchNumber = 0;
        goldenBatch = false;
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
    
    
    public void run() {
    	try {
			powerOn();	        
			startNewBatch();
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
      
    	while(state.equalsIgnoreCase("on")){
    		try {
    			//System.out.println("Cycle Randomizer: " + incident);
    			while(cyclesCompleted < 14){
    				if(mode.equalsIgnoreCase("training"))
    					runTrainingCycle();
    				else
    					runSimulationCycle();
    			}
    		} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		System.out.print("******************** Total Produced: " + ((cellsPerML * volume) * 0.8));
    		if(batchNumber > 1000){
    			try {
					powerOff();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    		try {
				if(state.equalsIgnoreCase("on")){
					startNewBatch();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
    	Charset utf8 = StandardCharsets.UTF_8;
    	try {
			Files.write(Paths.get("SimulatedTransactions.txt"), transactionList, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    public void runSimulationCycle() throws InterruptedException{
			advanceTime();
			cyclesCompleted++;
    }
    
    public void runTrainingCycle() throws InterruptedException{
			advanceTime();
			cyclesCompleted++;
    }
    
    public void advanceTime() throws InterruptedException{
    	System.out.println("*************** Advancing Time by 12 hours");
    	hoursFromStart +=12;
    	Integer incident = random.nextInt(6-1) + 1;
    	double cellGrowthCoefficient = 1.0;
    	if(phLevel < 7.0 && phLevel > 6.5){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.35;
    	}else if(phLevel <= 6.5 && phLevel > 6.2){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.45;
    	}else if(phLevel <= 6.2 && phLevel > 6.0){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.55;
    	}else if(phLevel <= 6.0 && phLevel > 5.8){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.65;
    	}else if(phLevel <= 5.8){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.70;
    	}
    	
    	if(disolvedOxygen < 0.20 && disolvedOxygen > 0.18){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.2;
    	}else if(disolvedOxygen <= 0.18 && disolvedOxygen > 0.16){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.3;
    	}else if(disolvedOxygen <= 0.16 && disolvedOxygen > 0.14){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.4;
    	}else if(disolvedOxygen <= 0.14 && disolvedOxygen > 0.12){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.5;
    	}else if(disolvedOxygen <= 0.12){
    		cellGrowthCoefficient = cellGrowthCoefficient - 0.6;
    	}
    	
    	System.out.println("*************** Batch Integrity Roll: " + incident);
    	System.out.println("*************** Cell Growth Coefficient: " + cellGrowthCoefficient);
    	System.out.println("*************** Cell Density Increase: " + cellsPerML + ((cellsPerML/2) * cellGrowthCoefficient));
    	//if(cellGrowthCoefficient >= 0.0){
    		cellsPerML = cellsPerML + ((cellsPerML/2) * cellGrowthCoefficient);
    	//}
    	glucoseLevel = glucoseLevel - ((cellsPerML/initialCellConcentration) * 0.02);
   	    lactateLevel = lactateLevel + ((cellsPerML/initialCellConcentration) * 0.01);
        mediaLevel = 0.75;
        if(incident == 4 && !goldenBatch){
        	phLevel = phLevel - ((lactateLevel/0.1) * 0.03);
        }else{
        	phLevel = phLevel - ((lactateLevel/0.1) * 0.01);
        }
        if(incident == 5 && !goldenBatch){
        	disolvedOxygen = disolvedOxygen - ((cellsPerML/initialCellConcentration) * 0.0015);
        }else{
        	disolvedOxygen = disolvedOxygen - ((cellsPerML/initialCellConcentration) * 0.0005);
        }
    	
        internalTemp += 0.1;
    	System.out.println("*************** Current Cell Concentration: " + cellsPerML);
        
    	if(mode.equalsIgnoreCase("simulation")){
    		sendStatus();
    		Thread.sleep(1000);
    	}else{
    		writeStatus();
    	}
    }
    
    public void startNewBatch() throws InterruptedException{
    	powerOn();
    	cyclesCompleted = 0;
    	hoursFromStart = 0;
    	batchNumber++;
    	System.out.println("*************** Starting New Batch");
    	glucoseLevel = 8.0;
        lactateLevel = 0.0;
        mediaLevel = 0.75;
        phLevel = 7.2;
        disolvedOxygen = 0.2;
        cellsPerML = 300000;
        internalTemp = 37.00; //Measured Celcius
        
        Integer goldenBatchRoll = random.nextInt(6-1) + 1;
        if(goldenBatchRoll == 5){
        	goldenBatch = true;
        }else{
        	goldenBatch = false;
        }
        
        if(mode.equalsIgnoreCase("simulation")){
    		sendStatus();
    		Thread.sleep(1000);
    	}else{
    		writeStatus();
    	}
    }
    
    public void sendStatus(){
        BioReactorStatus bioReactorStatus = new BioReactorStatus();
        bioReactorStatus.setSerialNumber(serialNumber);
        bioReactorStatus.setState(state);
        bioReactorStatus.setStatus(status);
        bioReactorStatus.setInternalTemp(internalTemp);
        bioReactorStatus.setDisolvedOxygen(disolvedOxygen);
        bioReactorStatus.setGlucoseLevel(glucoseLevel);
        bioReactorStatus.setLactateLevel(lactateLevel);
        bioReactorStatus.setPhLevel(phLevel);
        bioReactorStatus.setBatchNumber(batchNumber);
        bioReactorStatus.setHoursFromStart(hoursFromStart);
        bioReactorStatus.setCellsPerML(cellsPerML);
        
        try{
        	URL url = new URL("http://localhost:8082/contentListener");
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    		conn.setDoOutput(true);
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/json");
            
            System.out.println("To String: " + convertPOJOToJSON(bioReactorStatus));
            
            OutputStream os = conn.getOutputStream();
    		os.write(convertPOJOToJSON(bioReactorStatus).getBytes());
    		os.flush();
            
            if (conn.getResponseCode() != 200) {
    			throw new RuntimeException("Failed : HTTP error code : "
    				+ conn.getResponseCode());
    		}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void writeStatus(){
        BioReactorStatus bioReactorStatus = new BioReactorStatus();
        bioReactorStatus.setSerialNumber(serialNumber);
        bioReactorStatus.setState(state);
        bioReactorStatus.setStatus(status);
        bioReactorStatus.setInternalTemp(internalTemp);
        bioReactorStatus.setDisolvedOxygen(disolvedOxygen);
        bioReactorStatus.setGlucoseLevel(glucoseLevel);
        bioReactorStatus.setLactateLevel(lactateLevel);
        bioReactorStatus.setPhLevel(phLevel);
        bioReactorStatus.setHoursFromStart(hoursFromStart);
        bioReactorStatus.setBatchNumber(batchNumber);
        bioReactorStatus.setCellsPerML(cellsPerML);
        
        System.out.println("To String: " + convertPOJOToJSON(bioReactorStatus));
        
		transactionList.add(convertPOJOToJSON(bioReactorStatus));
    }
    
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
    public void setInternalTemp(double value){
        internalTemp = value;
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
    public double getInternalTemp(){
        return internalTemp;
    }
    public String getExternalCommand(){
        return externalCommand;
    }

	public double getGlucoseLevel() {
		return glucoseLevel;
	}

	public void setGlucoseLevel(double glucose) {
		this.glucoseLevel = glucose;
	}

	public double getLactateLevel() {
		return lactateLevel;
	}

	public void setLactateLevel(double lactate) {
		this.lactateLevel = lactate;
	}

	public double getDisolvedOxygen() {
		return disolvedOxygen;
	}

	public void setDisolvedOxygen(Integer disolvedOxygen) {
		this.disolvedOxygen = disolvedOxygen;
	}

	public double getPhLevel() {
		return phLevel;
	}

	public void setPhLevel(Integer phLevel) {
		this.phLevel = phLevel;
	}

	public double getMediaLevel() {
		return mediaLevel;
	}

	public void setMediaLevel(Integer mediaLevel) {
		this.mediaLevel = mediaLevel;
	}

	public double getCellsPerML() {
		return cellsPerML;
	}

	public void setCellsPerML(double cellsPerML) {
		this.cellsPerML = cellsPerML;
	}

	public Integer getVolume() {
		return volume;
	}

	public void setVolume(Integer volume) {
		this.volume = volume;
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