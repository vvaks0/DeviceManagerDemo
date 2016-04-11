package com.hortonworks.iot.simulator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.hortonworks.iot.rest.TechnicianService;
import com.hortonworks.iot.simulator.events.ProgramGuide;
import com.hortonworks.iot.simulator.events.Station;
import com.hortonworks.iot.simulator.types.BioReactorSimulator;
import com.hortonworks.iot.simulator.types.FiltrationSystemSimulator;
import com.hortonworks.iot.simulator.types.STBSimulator;
import com.hortonworks.iot.simulator.types.TechnicianSimulator;
import com.hortonworks.iot.simulator.types.X1TunerSimulator;

import net.sf.ehcache.CacheManager;

public class Simulator {
    // Base URI the Grizzly HTTP server will listen on
    public static String ipaddress;
    public static String port;
	public static HttpServer startServer(String simType, String deviceId) {
    	//Map<String,String> deviceDetailsMap = new HashMap<String, String>();
    	Map<String,String> deviceNetworkInfoMap = new HashMap<String, String>();
    	ResourceConfig config = null;
    	URI baseUri = null;
    	deviceNetworkInfoMap = getNetworkInfo(deviceId, simType);
    	baseUri = UriBuilder.fromUri("http://"+ deviceNetworkInfoMap.get("ipaddress") + "/server/").port(Integer.parseInt(deviceNetworkInfoMap.get("port"))).build();
    	//deviceDetailsMap = getSimulationDetails(simType, deviceId);
		//baseUri = UriBuilder.fromUri("http://" + deviceDetailsMap.get("ipaddress") + "/server/").port(Integer.parseInt(deviceDetailsMap.get("port"))).build();
	
    	if(simType.equalsIgnoreCase("STB")){
    		config = new ResourceConfig(TechnicianService.class);
    	}
    	else if(simType.equalsIgnoreCase("Technician")){
    		config = new ResourceConfig(TechnicianService.class);
    	}
    	else if(simType.equalsIgnoreCase("BioReactor")){
    		config = new ResourceConfig(TechnicianService.class);
    	}
    	else if(simType.equalsIgnoreCase("FiltrationSystem")){
    		config = new ResourceConfig(TechnicianService.class);
    	}
    	else{
    		System.exit(1);
    	}
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
		return server;
    }
    public static Map<String,String> getNetworkInfo(String deviceId, String simType){
    	Map<String,String> deviceNetworkInfoMap = new HashMap<String, String>();
        String ipaddress;
    	String hostname;
        String ipScratch[];
        try {
            ipScratch = InetAddress.getLocalHost().toString().replace("/", ":").split(":"); 
            ipaddress = InetAddress.getLocalHost().getHostAddress();
            hostname = InetAddress.getLocalHost().getHostName();
            deviceNetworkInfoMap.put("ipaddress", ipaddress);
            deviceNetworkInfoMap.put("hostname", hostname);
            System.out.println("Current IP address : " + ipaddress);
            System.out.println("Current Hostname : " + hostname);         
        } catch (UnknownHostException e) {
 
            e.printStackTrace();
        }
        
        switch(simType + " " + deviceId){
		case "Technician 1000":
			deviceNetworkInfoMap.put("port", "8084");
			break;
		case "Technician 2000":
			deviceNetworkInfoMap.put("port", "8086");
			break;
		case "Technician 3000":
			deviceNetworkInfoMap.put("port", "8088");
			break;
		case "STB 1000":
			deviceNetworkInfoMap.put("port", "8085");
			break;
		case "STB 2000":
			deviceNetworkInfoMap.put("port", "8087");
			break;
		case "STB 3000":
			deviceNetworkInfoMap.put("port", "8089");
			break;
		case "BioReactor 1000":
			deviceNetworkInfoMap.put("port", "8085");
			break;
		case "BioReactor 2000":
			deviceNetworkInfoMap.put("port", "8087");
			break;
		case "BioReactor 3000":
			deviceNetworkInfoMap.put("port", "8089");
			break;
		case "FiltrationSystem 1000":
			deviceNetworkInfoMap.put("port", "8070");
			break;
		case "FiltrationSystem 2000":
			deviceNetworkInfoMap.put("port", "8071");
			break;
		case "FiltrationSystem 3000":
			deviceNetworkInfoMap.put("port", "8072");
			break;
		default:
			System.out.println("There is no record of " + simType + " " + deviceId + ". Cannot start device simulation");
			System.exit(1);
			break;
		}
        
        return deviceNetworkInfoMap;
    }
    
	public static void main(String[] args) throws IOException {
		Thread deviceThread;
		Thread techThread;
		String simType = args[0];
		String serialNumber = args[1];
		String mode = args[2];
		System.out.println("Starting Cache...");
		CacheManager.create();
		CacheManager.getInstance().addCache("TechnicianRouteRequest");
		
		if(simType.equalsIgnoreCase("STB")){
			System.out.println("Starting Webservice...");
			final HttpServer server = startServer(simType, serialNumber);
			server.start();
			System.out.println("Starting Set Top Box...");
			Map networkInfo = getNetworkInfo(serialNumber, simType);
			STBSimulator stb = new STBSimulator(serialNumber, mode);
            deviceThread = new Thread(stb);
            deviceThread.setName("Device: " + serialNumber);
            deviceThread.start();
		}
		else if(simType.equalsIgnoreCase("Technician")){			
			System.out.println("Starting Webservice...");
			final HttpServer server = startServer(simType, serialNumber);
			server.start();
			System.out.println("Starting Technician Route");
			Map networkInfo = getNetworkInfo(serialNumber, simType);
			ipaddress =  (String)networkInfo.get("ipaddress");
			port =  (String)networkInfo.get("port");
			TechnicianSimulator tech = new TechnicianSimulator(serialNumber, ipaddress, port);
            techThread = new Thread(tech);
            techThread.setName("Technician: " + serialNumber);
            techThread.start();
        }
		else if(simType.equalsIgnoreCase("X1Tuner")){			
			System.out.println("Starting X1 Channel Tune Event Simualtion");
			int numDevices = Integer.valueOf(args[3]) * 1000;
			int lastSerialNumber = numDevices;
			int[] zipCodes = {19101, 33301, 80123, 94105, 97201};
			//ipaddress =  (String)networkInfo.get("ipaddress");
			//port =  (String)networkInfo.get("port");
			Map<Integer,String> market = new HashMap<Integer, String>(); 
			market.put(19101, "Philadelphia, PA");
			market.put(33301, "Ft. Lauderdale, FL");
			market.put(80123, "Denver, CO");
			market.put(94105, "San Francisco, CA");
			market.put(97201, "Portland, OR");
			for(int zipCode: zipCodes){
				List<Station> guide = new ArrayList<Station>();
				guide = ProgramGuide.getProgramGuide(zipCode);
				for(int currentSerialNumber=1000; currentSerialNumber<=lastSerialNumber; currentSerialNumber+=1000){
					X1TunerSimulator xOneTune = new X1TunerSimulator(guide, currentSerialNumber+""+zipCode, mode, market.get(zipCode), zipCode);
					techThread = new Thread(xOneTune);
					techThread.setName("Device: " + serialNumber + zipCode);
					techThread.start();
				}
			}
        }
		else if(simType.equalsIgnoreCase("BioReactor")){			
			System.out.println("Starting Webservice...");
			final HttpServer server = startServer(simType, serialNumber);
			server.start();
			System.out.println("Starting BioReactor Fermentation Process...");
			Map networkInfo = getNetworkInfo(serialNumber, simType);
			ipaddress =  (String)networkInfo.get("ipaddress");
			port =  (String)networkInfo.get("port");
			//BioReactorSimulator bioReactor = new BioReactorSimulator(serialNumber, ipaddress, port);
			BioReactorSimulator bioReactor = new BioReactorSimulator(serialNumber, mode);
			deviceThread = new Thread(bioReactor);
			deviceThread.setName("BioReactor: " + serialNumber);
			deviceThread.start();
        }else if(simType.equalsIgnoreCase("FiltrationSystem")){			
			System.out.println("Starting Webservice...");
			final HttpServer server = startServer(simType, serialNumber);
			server.start();
			System.out.println("Starting Filtration Process...");
			Map networkInfo = getNetworkInfo(serialNumber, simType);
			ipaddress =  (String)networkInfo.get("ipaddress");
			port =  (String)networkInfo.get("port");
			//BioReactorSimulator bioReactor = new BioReactorSimulator(serialNumber, ipaddress, port);
			FiltrationSystemSimulator filtartionSystem = new FiltrationSystemSimulator(serialNumber, mode);
			deviceThread = new Thread(filtartionSystem);
			deviceThread.setName("Filtration System: " + serialNumber);
			deviceThread.start();
        }
    }
	
    /*
    public static Map<String,String> getSimulationDetails(String simType, String deviceId){
    	Map<String,String> deviceDetailsMap = new HashMap<String, String>();
    	Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "localhost");
		config.set("hbase.zookeeper.property.clientPort", "2181");
		config.set("zookeeper.znode.parent", "/hbase-unsecure");
		
		//System.out.println("Create Config...");
	    // Instantiating HTable class
	    HTable table = null;
		try {
			table = new HTable(config, "DeviceDetails");
		} catch (IOException e) {
			e.printStackTrace();
		}
	    //System.out.println("Get Table...");
	    // Instantiating Get class
	    Get get = new Get(Bytes.toBytes(deviceId));
	    System.out.println("Build Request...");
	    // Reading the data
	    Result result = null;
		try {
			result = table.get(get);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    //System.out.println("Get Results...");
	    // Reading values from Result class object
		
		if(result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("ipaddress")) !=null && result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("port")) != null){
			byte [] ipaddress = result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("ipaddress"));
			byte [] port = result.getValue(Bytes.toBytes("cf"),Bytes.toBytes("port"));
			deviceDetailsMap.put("ipaddress", ipaddress.toString());
			deviceDetailsMap.put("port", port.toString());
			
			return deviceDetailsMap;
		}
		else{
			System.out.println("There is no record of Device " + deviceId + " in HBase. Cannot start device simulation");
			return null;
		}
    } */
}
