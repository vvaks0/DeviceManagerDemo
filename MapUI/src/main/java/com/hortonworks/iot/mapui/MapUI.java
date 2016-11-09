package com.hortonworks.iot.mapui;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;

@WebServlet(name = "MapUI", urlPatterns = { "/DeviceMap" })
public class MapUI extends HttpServlet {
    private static final String CONTENT_TYPE = "text/html; charset=windows-1252";
    private String requestType;
    private String zkHost = "sandbox.hortonworks.com";
    private String zkPort = "2181";
    private String zkHBasePath = "/hbase-unsecure";
    private String httpHost = "sandbox.hortonworks.com";
    private String httpListenPort = "8082";
    private String httpListenUri = "/contentListener";
    private String cometdHost = "sandbox.hortonworks.com";
    private String cometdListenPort = "8091";
	private String defaultAccountNumber = "19123";
	private String mapAPIKey = "NO_API_KEY_FOUND";
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("Calling Init method and setting request to Initial");
        requestType = "initial";
    }

    public void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();
        System.out.println("First Check of Request Type: " + requestType);
        if(request.getParameter("requestType") != null){
            System.out.println("RequestType parameter : " + request.getParameter("requestType"));
            requestType = request.getParameter("requestType");
            System.out.println("RequestType set to :" + requestType);
        }
        
        //testPubSub();
        
        Map<String, String> env = System.getenv();
        System.out.println("********************** ENV: " + env);
        if(env.get("ZK_HOST") != null){
        	this.zkHost = (String)env.get("ZK_HOST");
        }
        if(env.get("ZK_PORT") != null){
        	this.zkPort = (String)env.get("ZK_PORT");
        }
        if(env.get("ZK_HBASE_PATH") != null){
        	this.zkHBasePath = (String)env.get("ZK_HBASE_PATH");
        }
        if(env.get("COMETD_HOST") != null){
        	this.cometdHost = (String)env.get("COMETD_HOST");
        }
        if(env.get("COMETD_PORT") != null){
        	this.cometdListenPort = (String)env.get("COMETD_PORT");
        }
        if(env.get("HTTP_HOST") != null){
        	this.httpHost = (String)env.get("HTTP_HOST");
        }
        if(env.get("HTTP_PORT") != null){
        	this.httpListenPort = (String)env.get("HTTP_PORT");
        }
        if(env.get("HTTP_URI") != null){
        	this.httpListenUri = (String)env.get("HTTP_URI");
        }
        if(env.get("MAP_API_KEY") != null){
        	this.mapAPIKey  = (String)env.get("MAP_API_KEY");
        }
        System.out.println("********************** Zookeeper Host: " + zkHost);
        System.out.println("********************** Zookeeper: " + zkPort);
        System.out.println("********************** Zookeeper Path: " + zkHBasePath);
        System.out.println("********************** Cometd Host: " + cometdHost);
        System.out.println("********************** Cometd Port: " + cometdListenPort);
        System.out.println("********************** Http Host: " + httpHost);
        System.out.println("********************** Http Port: " + httpListenPort);
        System.out.println("********************** Http Uri: " + httpListenUri);
        System.out.println("********************** Map Api Key: " + mapAPIKey);
        
        ServletContext sc = this.getServletContext();
        
        URL indexUri = this.getClass().getResource("/webapp");
        //System.out.println(indexUri.toExternalForm());
        System.out.println("Checking if Initial: " + requestType);
        if(requestType.equalsIgnoreCase("initial")){
        	request.setAttribute("cometdHost", cometdHost);
        	request.setAttribute("cometdPort", cometdListenPort);
        	request.setAttribute("mapAPIKey", mapAPIKey);
            request.getRequestDispatcher("mapui.jsp").forward(request, response);
        }
    }
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doTask(request, response);
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
       this.doTask(request, response);
    }
    
    public void testPubSub() {
    	String pubSubUrl = "http://sandbox.hortonworks.com:8091/cometd";
    	String deviceChannel = "/devicestatus";
    	HttpClient httpClient = new HttpClient();
		try {
			httpClient.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Prepare the transport
		Map<String, Object> options = new HashMap<String, Object>();
		ClientTransport transport = new LongPollingTransport(options, httpClient);

		// Create the BayeuxClient
		BayeuxClient bayuexClient = new BayeuxClient(pubSubUrl, transport);
		
		bayuexClient.handshake();
		boolean handshaken = bayuexClient.waitFor(5000, BayeuxClient.State.CONNECTED);
		if (handshaken)
		{
			System.out.println("Connected to Cometd Http PubSub Platform");
		}
		else{
			System.out.println("Could not connect to Cometd Http PubSub Platform");
		}
		
		bayuexClient.getChannel(deviceChannel).publish("TEST");
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
}