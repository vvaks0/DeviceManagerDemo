package com.hortonworks.iot.simulator.events;

import java.io.Serializable;

import com.hortonworks.iot.simulator.events.DeviceStatus;

public class STBStatus extends DeviceStatus implements Serializable {
	private static final long serialVersionUID = 1L;
	private Integer internalTemp;
	private Integer signalStrength;
    
	public void setInternalTemp(Integer value){
        internalTemp = value;
    }
    public void setSignalStrength(Integer value){
        signalStrength = value;
    }
    
    public Integer getInternalTemp(){
        return internalTemp;
    }
    public Integer getSignalStrength(){
        return signalStrength;
    }
}