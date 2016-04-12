# DeviceManagerDemo
The Device Manager Demo is designed to demonstrate a fully functioning modern Data/IoT application. It is a Lambda architecture built using the Hortonworks Data Platform and Hortonworks Data Flow. The demo shows how a Telecom can manage customer device outages using predictive maintenance and a connected workforce.

Download and Import Hortonworks Sandbox 2.3.2 for Virtual Box. Should work with VMWare but has not been tested.
Modify local hosts file so that sandbox.hortonworks.com resolves to 127.0.0.1 (This is important and may break the simulator and UI)
Start Sandbox, SSH to Sandbox, Change sandbox root password

From Ambari (http://sandbox.hortonworks.com:8080)
 
- Start Kafka (This is important, if Kafka is not started, the install script will not be able to configure the required Kafka Topics)

ssh to sandbox as root into /root directory  

git clone https://github.com/vakshorton/DeviceManagerDemo.git (make sure that git cloned to /root/DeviceManagerDemo)

cd DeviceManagerDemo

chmod 755 install.sh

./install.sh

From Ambari

 - Increase Yarn memory per container to at least 5GB (This is important as the default setting of 2GB is not enough to support the application servers on Yarn)
 
- Install Nifi using Add Service button

Reboot Sandbox

Configure Virtual Box Port Forward

8082 – HDF_HTTP_Ingest

8090 - MapUI

8091 - Cometd

9090 – HDF_Studio

From Ambari Admin 

 - start Nifi, Hbase, Kafka, Storm

From the NiFi Studio interface (http://sandbox.hortonworks.com:9090/nifi), Import DeviceManageDemo.xml as a template into Nifi (The template is in the NifiFlow floder. Nifi allows you to browse the local machine so it may be easier to download a copy locally directly from git)

Make sure to start all of the processors, should just need to hit the green start button as all of the processors will be selected after import

Make sure that docker is running: service docker status. If not, start it: service docker start (This is important as the UI is launched by Slider as a docker container)

Start Application Servers on Slider:

slider create mapui --template /usr/hdp/docker/dockerbuild/mapui/appConfig.json --metainfo /usr/hdp/docker/dockerbuild/mapui/metainfo.json --resources /usr/hdp/docker/dockerbuild/mapui/resources.json

(Slider will download the docker containers from the docker hub so it may take a few minutes for the application server to start)

Deploy Storm Topology:

storm jar /home/storm/DeviceMonitor-0.0.1-SNAPSHOT.jar com.hortonworks.iot.topology.DeviceMonitorTopology

Start Spark Nostradamus:

spark-submit --class com.hortonworks.iot.spark.streaming.SparkNostradamus --master local[4] /home/spark/DeviceMonitorNostradamus-0.0.1-SNAPSHOT-jar-with-dependencies.jar


Build Simulator inside of Sandbox 

cd /root/DeviceManagerDemo/DeviceSimulator

mvn clean package

scp target/DeviceSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar to the local machine

Start Simulation on Host (Not inside VM):
USAGE:

java -jar simulator.jar arg1=Simulator-Type{STB,Technician,X1Simulator} arg2=EntityId arg3={Simulation|Training} Only for X1 Simulator(arg4={Number of threads per market}

Set Top Box

java -jar {PATH_TO_SIMULATOR.JAR}/DeviceSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar "STB” "1000" "Simulation"

Technician
java -jar {PATH_TO_SIMULATOR.JAR}/DeviceSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar “Technician” "1000" “Simulation"

X1 Simulator

java -jar {PATH_TO_SIMULATOR.JAR}//DeviceSimulator-0.0.1-SNAPSHOT-jar-with-dependencies.jar “X1Tuner” "1000" “Simulation” 10
