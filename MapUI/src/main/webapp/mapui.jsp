<html>
<head>
<title>Device Monitor Map</title>
<!-- <link rel="stylesheet" type="text/css" href="css/mapStyle.css"> -->
<style type="text/css">
body {
    margin: 0;
    padding: 0;
}

.header{
	padding-top: 10px;
    padding-bottom: 10px;
    vertical-align: bottom;
    position: relative;
    height: 70px;
    background-color: #333;
    border-bottom: 5px solid #3FAB2A;
}

#brandingLayout {
    position: relative;
    height: 70px;
    padding: 0px 48px 0px 10px;
}

.brandTitle {
    color: #ffffff;
    font-weight: bolder;
    font-size: 1.25em;
    vertical-align: bottom;
    margin-left: 2px;
    margin-right: 0px;
}

.sidePane{
    float: right;
    height: 100%;
    width: 20%;
    background-color: #ffffff;
    /*box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.1)*/
}

.techDiv{
    padding: 20px; 
    margin: 15px;
    width: 75%;
    height: 100px;
    margin-top: 15px;
    background: #ffffff;
    border-top: 1px solid #D4D4D4;
    border-left: 1px solid #D4D4D4;
    border-bottom: 1px solid #D4D4D4;
    border-right: 1px solid #D4D4D4;
    /*box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.1);*/
    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 14px
}

.deviceContainer{
	margin-top: 5px;
    float: left;
    margin: 10px;
    width: 31%;
    height: 200px;
}

.deviceAlertDiv{
	margin-top: 5px;
    border: 5px solid #f5f5f5;
    background: #ffffff;
    padding: 5px;
    float: left;
    margin: 5px;
    width: 100%;
    height: 30%;
    border-top: 1px solid #D4D4D4;
    border-left: 1px solid #D4D4D4;
    border-bottom: 1px solid #D4D4D4;
    border-right: 1px solid #D4D4D4;
    /*box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.1);*/
    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 14px
}
.deviceDiv{
	margin-top: 5px;
    border: 5px solid #f5f5f5;
    background: #ffffff;
    padding: 5px;
    float: left;
    margin: 5px;
    width: 100%;
    height: 50%;
    border-top: 1px solid #D4D4D4;
    border-left: 1px solid #D4D4D4;
    border-bottom: 1px solid #D4D4D4;
    border-right: 1px solid #D4D4D4;
    /*box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.1);*/
    font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 14px
}

div#bodyContainer{
	margin-top: 15px;
	height: 450px;
}
div#mapcontainer{
	padding-top: 10px;
    padding-bottom: 10px;
    height: 100%;
	width:80%;
	float:left
}
div#lowerContainer{
	margin-top: 10px;
	height: 600px;
    margin: 0;
    padding: 0;
	overflow: none;
}

div#deviceContainer{
    padding-bottom: 10px;
    height: 200px;
    width: 100%;
    float: left;
    border-bottom: 1px solid #ffffff;
    border-right: 1px solid #ffffff;
    /*box-shadow: 5px 5px 5px rgba(0, 0, 0, 0.1)*/
}
div#device{
	float:left;
}
div#technician{
	margin: auto;
	width: 70%;
}
div#command{
	position: relative;
	top: 10px;
}

</style>
<script type="text/javascript" src="https://maps.googleapis.com/maps/api/js?key=${mapAPIKey}"></script>
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.7.8/dojo/dojo.js"></script>
<script type="text/javascript">
  dojo.require("dojo.io.script");
  dojo.require("dojox.cometd");
  dojo.require("dojox.cometd.longPollTransport");
  
  var cometdHost = "${cometdHost}";
  var cometdPort = "${cometdPort}";
  var pubSubUrl = "http://" + cometdHost + ":" + cometdPort + "/cometd"; 
  var deviceChannel = "/devicestatus";
  var technicianChannel = "/technicianstatus";
  var alertChannel = "/alert";
  var predictionChannel = "/prediction";
  var latlang;
  var lat = 39.957583;
  var lng = -75.162320;
  var contentString;
  var greenIcon = "images/stb_normal_25.png";
  var yellowIcon = "images/stb_warning_25.png";
  var redIcon = "images/stb_alert_25.png";
  var carIcon = {url:"images/car.png",size: new google.maps.Size(30, 30),origin: new google.maps.Point(0, 0),anchor: new google.maps.Point(15, 15)};
  var map;
  var marker;
  var technician;
  var techDiv;
  var alert = {};
  var markers = {};
  var technicians = {};
  var markerInfo = {};
  
  function loadMap() {
    var myOptions = {
      zoom: 13,
      center: new google.maps.LatLng(lat, lng),
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById("mapcontainer"),myOptions);
  }
  
  dojo.ready(connectMedicalDeviceTopic)
  
	function connectMedicalDeviceTopic(){
  		dojox.cometd.init(pubSubUrl);

  		dojox.cometd.subscribe("/*", function(message){
  			if(message.channel == deviceChannel){
  				console.log(message);
  				
  				if(message.data.deviceSerialNumber == null){
  					var serial_number = message.data.serial_number
  				}else{
  					var serial_number = message.data.deviceSerialNumber
  				}
  				
  				if(message.data.deviceModel == null){
  					var device_model = message.data.device_model
  				}else{
  					var device_model = message.data.deviceModel
  				}
  				
  				if(message.data.signalStrength == null){
  					var signal_strength = message.data.signal_strength
  				}else{
  					var signal_strength = message.data.signalStrength
  				}
  				
  				if(message.data.internalTemp == null){
  					var internal_temp = message.data.internal_temp
  				}else{
  					var internal_temp = message.data.internalTemp
  				}
  				
  				if (typeof message.data.latitude === 'string' || message.data.latitude instanceof String){
  					var latitude = parseFloat(message.data.latitude)
  				}else{
  					var latitude = message.data.latitude
  				}
  				
  				if (typeof message.data.longitude === 'string' || message.data.longitude instanceof String){
  					var longitude = parseFloat(message.data.longitude)
  				}else{
  					var longitude = message.data.longitude
  				}
  				
  				contentString = 'Serial Number: ' + serial_number + '<br>' +
  						'Device Model: ' + device_model + '<br>' + 
  						//'Device Status: ' + message.data.status + '<br>' +  
  						'Signal Strength: ' + signal_strength + '<br>' + 
  						'Internal Temp: ' + internal_temp + '<br>' // +
  						//'Device State: ' + message.data.state;

  						//if(alert[message.data.deviceSerialNumber] != null)
  							//contentString = contentString + alert[message.data.deviceSerialNumber];

  						if(!markers[serial_number]){
  							markers[serial_number] = new google.maps.Marker({position: {lat: latitude, lng: longitude}, map: map, icon: greenIcon}); 
  							markers[serial_number].setTitle(serial_number);
  							markerInfo[serial_number] = new google.maps.InfoWindow({content: contentString});
  							markerInfo[serial_number].setContent(contentString);
  							google.maps.event.addListener(markers[serial_number], 'click', function() {markerInfo[serial_number].open(map,markers[serial_number]); } );  
  							document.getElementById("device" + serial_number).innerHTML = contentString;
  							console.log(markers[serial_number].getTitle());
  						}
  						else{
  							markerInfo[serial_number].setContent(contentString);
  							document.getElementById("device" + serial_number).innerHTML = contentString;
  						}
  			}
  			else if(message.channel == technicianChannel){
  				console.log(message);
  				
  				if(message.data.technicianId == null){
  					var technician_id = message.data.technician_id
  				}else{
  					var technician_id = message.data.technicianId
  				}
  				
  				if(message.data.status == null){
  					var status = 'undefined'
  				}else{
  					var status = message.data.status
  				}
  				
  				if (typeof message.data.latitude === 'string' || message.data.latitude instanceof String){
  					var latitude = parseFloat(message.data.latitude)
  				}else{
  					var latitude = message.data.latitude
  				}
  				
  				if (typeof message.data.longitude === 'string' || message.data.longitude instanceof String){
  					var longitude = parseFloat(message.data.longitude)
  				}else{
  					var longitude = message.data.longitude
  				}
  				
  				contentString = 'Technician Name: ' + technician_id + '<br>' +
					'Latitude: ' + latitude + '<br>' + 
					'Longitude: ' + longitude + '<br>' + 
					'Status: ' + status + '<br>';
  	        	if(!technicians[technician_id]){
  	        		technicians[technician_id] = new google.maps.Marker({position: {lat: lat, lng: lng}, map: map, icon: carIcon});
  	        		technicians[technician_id].setTitle(technician_id);
  	        		technicians[technician_id].setPosition({lat: latitude, lng: longitude});
  	  
  	        		document.getElementById("tech" + technician_id).innerHTML = contentString;
  	        		//technicians[message.data.technicianId] = new google.maps.InfoWindow({content: contentString});
  	        		//technicians[message.data.technicianId].setContent(contentString);
  	        		//google.maps.event.addListener(technicians[message.data.technicianId], 'click', function() {technicians[message.data.technicianId].open(map,technicians[message.data.technicianId]); } );  
  	        		console.log(technicians[technician_id].getTitle());		 
  	        	}
  	        	else{
  	        		technicians[technician_id].setPosition({lat: latitude, lng: longitude});
  					document.getElementById("tech" + technician_id).innerHTML = contentString;
  				}
  			}
  			else if(message.channel == alertChannel){
  				
  				if(message.data.deviceSerialNumber == null){
  					var serial_number = message.data.serial_number
  				}else{
  					var serial_number = message.data.deviceSerialNumber
  				}
  				
  				if(message.data.alertDescription == null){
  					var alert_description = message.data.alert_description
  				}else{
  					var alert_description = message.data.alertDescription
  				}
  				
  				console.log("ALERT: " + serial_number + " : " + alert_description);
  				markers[serial_number].setIcon(redIcon);
  				alert[serial_number] = '<br><font color="red">ALERT: ' + alert_description + '</font><br>';
  				document.getElementById("deviceAlert" + serial_number).innerHTML = '<br><font color="red">ALERT: ' + alert_description + '</font><br>';
  			}
  			else if(message.channel == predictionChannel){
  				
  				if(message.data.deviceSerialNumber == null){
  					var serial_number = message.data.serial_number
  				}else{
  					var serial_number = message.data.deviceSerialNumber
  				}
  				
  				if(message.data.predictionDescription == null){
  					var prediction_description = message.data.prediction_description
  				}else{
  					var prediction_description = message.data.predictionDescription
  				}
  				
  				console.log("WARNING: " + message.data.serial_number + " : " + prediction_description);
  				//markers[message.data.deviceSerialNumber].setIcon(redIcon);
  				alert[serial_number] = '<br><font color="orange">WARNING: ' + prediction_description + '</font><br>';
  				document.getElementById("deviceAlert" + serial_number).innerHTML = '<br><font color="orange">WARNING: ' + prediction_description + '</font><br>';;
  			}
  		});
  	}

  function driveRoute(lat, lng){ 
		technician.setPosition({lat:lat, lng:lng});
		stepCount++;
		if(stepCount%5==0){
			map.setCenter({lat:lat, lng:lng});
		}
  }
</script>
</head>
 
<body onload="loadMap()">
<div class="header">
		<div id="brandingLayout">
                <a class="brandingContent" href="CustomerOverview?requestType=customerOverview">
                    <img src="images/hortonworks-logo-new.png" width="200px"/>
                    <span class="brandTitle" data-i18n="BRAND_TITLE"></span>
                </a>
		</div>
</div>
<div id="bodyContainer">
	<div id="mapcontainer"></div>
	<div id="sidePane" class="sidePane">
		<div id="tech1000" class="techDiv"></div>
		<div id="tech2000" class="techDiv"></div>
		<div id="tech3000" class="techDiv"></div>
	</div>
</div>  
	<div id="lowerContainer">  
    	<div id="deviceContainer">
    		<div id="container1000" class="deviceContainer">
    			<div id="device1000" class="deviceDiv"></div>
				<div id="deviceAlert1000" class="deviceAlertDiv"></div>
			</div>
			<div id="container2000" class="deviceContainer">
				<div id="device2000" class="deviceDiv"></div>
    			<div id="deviceAlert2000" class="deviceAlertDiv"></div>
    		</div>
    		<div id="container3000" class="deviceContainer">
    			<div id="device3000" class="deviceDiv"></div>
    			<div id="deviceAlert3000" class="deviceAlertDiv"></div>
    		</div>
    	</div>
    </div>		
</body>
 
</html>