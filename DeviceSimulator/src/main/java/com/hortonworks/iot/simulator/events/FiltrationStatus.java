package com.hortonworks.iot.simulator.events;

import java.io.Serializable;

public class FiltrationStatus extends DeviceStatus implements Serializable {
	private static final long serialVersionUID = 1L;
    private Integer internalPressure;
    private Integer flowRate;
    private Integer flowTemp;
    private Integer batchNumber;
    private Integer hoursFromStart;
	
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
