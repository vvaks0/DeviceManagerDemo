package com.hortonworks.iot.simulator.events;

import java.io.Serializable;

public class BioReactorStatus extends DeviceStatus implements Serializable {
		private static final long serialVersionUID = 1L;
		private double internalTemp;
		private double glucoseLevel;
		private double lactateLevel;
	    private double mediaLevel;
	    private double phLevel;
	    private double disolvedOxygen;
	    private double cellsPerML;
	    private Integer batchNumber;
	    private Integer hoursFromStart;
	    
		public void setInternalTemp(double value){
	        internalTemp = value;
	    }
	    public double getInternalTemp(){
	        return internalTemp;
	    }
		public double getMediaLevel() {
			return mediaLevel;
		}
		public void setMediaLevel(double mediaLevel) {
			this.mediaLevel = mediaLevel;
		}
		public double getPhLevel() {
			return phLevel;
		}
		public void setPhLevel(double phLevel) {
			this.phLevel = phLevel;
		}
		public double getDisolvedOxygen() {
			return disolvedOxygen;
		}
		public void setDisolvedOxygen(double disolvedOxygen) {
			this.disolvedOxygen = disolvedOxygen;
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
		public double getCellsPerML() {
			return cellsPerML;
		}
		public void setCellsPerML(double cellsPerML) {
			this.cellsPerML = cellsPerML;
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