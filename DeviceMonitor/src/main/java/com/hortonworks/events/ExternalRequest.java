package com.hortonworks.events;

import java.io.Serializable;

public class ExternalRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String serialNumber;
    private String command;

    public ExternalRequest() {}
    
    public void setDeviceSerialNumber(String value){
        serialNumber = value;
    }
    
    public String getDeviceSerialNumber(){
        return serialNumber;
    }
    
    public void setCommand(String value){
        command = value;
    }
    
    public String getCommand(){
        return command;
    }
}
