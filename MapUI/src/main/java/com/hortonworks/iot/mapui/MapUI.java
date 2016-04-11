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
        
        testPubSub();
        
        ServletContext sc = this.getServletContext();
        
        URL indexUri = this.getClass().getResource("/webapp");
        //System.out.println(indexUri.toExternalForm());
        System.out.println("Checking if Initial: " + requestType);
        if(requestType.equalsIgnoreCase("initial")){   
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