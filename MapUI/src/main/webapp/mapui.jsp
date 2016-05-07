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
<script type="text/javascript" src="http://maps.googleapis.com/maps/api/js?sensor=false"></script>
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.7.8/dojo/dojo.js"></script>
<script type="text/javascript">
  dojo.require("dojo.io.script");
  dojo.require("dojox.cometd");
  dojo.require("dojox.cometd.longPollTransport");
  
  var pubSubUrl = "http://localhost:8091/cometd"; 
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
  				contentString = 'Serial Number: ' + message.data.deviceSerialNumber + '<br>' +
  						'Device Model: ' + message.data.deviceModel + '<br>' + 
  						'Device Status: ' + message.data.status + '<br>' +  
  						'Signal Strength: ' + message.data.signalStrength + '<br>' + 
  						'Internal Temp: ' + message.data.internalTemp + '<br>' +
  						'Device State: ' + message.data.state;

  						//if(alert[message.data.deviceSerialNumber] != null)
  							//contentString = contentString + alert[message.data.deviceSerialNumber];

  						if(!markers[message.data.deviceSerialNumber]){
  							markers[message.data.deviceSerialNumber] = new google.maps.Marker({position: {lat: message.data.latitude, lng: message.data.longitude}, map: map, icon: greenIcon}); 
  							markers[message.data.deviceSerialNumber].setTitle(message.data.deviceSerialNumber);
  							markerInfo[message.data.deviceSerialNumber] = new google.maps.InfoWindow({content: contentString});
  							markerInfo[message.data.deviceSerialNumber].setContent(contentString);
  							google.maps.event.addListener(markers[message.data.deviceSerialNumber], 'click', function() {markerInfo[message.data.deviceSerialNumber].open(map,markers[message.data.deviceSerialNumber]); } );  
  							document.getElementById("device" + message.data.deviceSerialNumber).innerHTML = contentString;
  							console.log(markers[message.data.deviceSerialNumber].getTitle());
  						}
  						else{
  							markerInfo[message.data.deviceSerialNumber].setContent(contentString);
  							document.getElementById("device" + message.data.deviceSerialNumber).innerHTML = contentString;
  						}
  			}
  			else if(message.channel == technicianChannel){
  				console.log(message);
  				contentString = 'Technician Name: ' + message.data.technicianId + '<br>' +
					'Latitude: ' + message.data.latitude + '<br>' + 
					'Longitude: ' + message.data.longitude + '<br>' + 
					'Status: ' + message.data.status + '<br>';
  	        	if(!technicians[message.data.technicianId]){
  	        		technicians[message.data.technicianId] = new google.maps.Marker({position: {lat: lat, lng: lng}, map: map, icon: carIcon});
  	        		technicians[message.data.technicianId].setTitle(message.data.technicianId);
  	        		technicians[message.data.technicianId].setPosition({lat: message.data.latitude, lng: message.data.longitude});
  	  
  	        		document.getElementById("tech" + message.data.technicianId).innerHTML = contentString;
  	        		//technicians[message.data.technicianId] = new google.maps.InfoWindow({content: contentString});
  	        		//technicians[message.data.technicianId].setContent(contentString);
  	        		//google.maps.event.addListener(technicians[message.data.technicianId], 'click', function() {technicians[message.data.technicianId].open(map,technicians[message.data.technicianId]); } );  
  	        		console.log(technicians[message.data.technicianId].getTitle());		 
  	        	}
  	        	else{
  	        		technicians[message.data.technicianId].setPosition({lat: message.data.latitude, lng: message.data.longitude});
  					document.getElementById("tech" + message.data.technicianId).innerHTML = contentString;
  				}
  			}
  			else if(message.channel == alertChannel){
  				console.log("ALERT: " + message.data.deviceSerialNumber + " : " + message.data.alertDescription);
  				markers[message.data.deviceSerialNumber].setIcon(redIcon);
  				alert[message.data.deviceSerialNumber] = '<br><font color="red">ALERT: ' + message.data.alertDescription + '</font><br>';
  				document.getElementById("deviceAlert" + message.data.deviceSerialNumber).innerHTML = '<br><font color="red">ALERT: ' + message.data.alertDescription + '</font><br>';
  			}
  			else if(message.channel == predictionChannel){
  				console.log("WARNING: " + message.data.deviceSerialNumber + " : " + message.data.predictionDescription);
  				//markers[message.data.deviceSerialNumber].setIcon(redIcon);
  				alert[message.data.deviceSerialNumber] = '<br><font color="orange">WARNING: ' + message.data.predictionDescription + '</font><br>';
  				document.getElementById("deviceAlert" + message.data.deviceSerialNumber).innerHTML = '<br><font color="orange">WARNING: ' + message.data.predictionDescription + '</font><br>';;
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