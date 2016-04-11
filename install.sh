#!/bin/bash

#Copy binaries and files to required folders
mv /etc/init.d/solr /etc/init.d/solr_bak
tee /etc/init.d/solr <<-'EOF'
#!/bin/sh
### BEGIN INIT INFO
# Provides:          solr
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Description:       Controls Apache Solr as a Service
### END INIT INFO

SOLR_INSTALL_DIR=/opt/lucidworks-hdpsearch/solr

if [ ! -d "$SOLR_INSTALL_DIR" ]; then
  echo "$SOLR_INSTALL_DIR not found! Please check the SOLR_INSTALL_DIR setting in your $0 script."
  exit 1
fi

case "$1" in
start)
    $SOLR_INSTALL_DIR/bin/solr $1 -c -z sandbox.hortonworks.com
    ;;
stop)
    $SOLR_INSTALL_DIR/bin/solr $1
    ;;
status)
     $SOLR_INSTALL_DIR/bin/solr $1
    ;;
*)
    echo $"Usage: $0 {start|stop|status}"
    exit 2
esac
EOF
chmod 755 /etc/init.d/solr

#Build Storm Project and Copy to working folder
cd DeviceMonitor
mvn clean package 
cp target/DeviceMonitor-0.0.1-SNAPSHOT.jar /home/storm

#Build Spark Project and Copy to working folder
cd ../DeviceMonitorNostradamus
mvn clean package
cp target/DeviceMonitorNostradamus-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/spark

#Import Zeppelin Notebook
cd ../Notebook
unzip notebook.zip -d /opt/incubator-zeppelin/notebook/
chown -R zeppelin:hadoop /opt/incubator-zeppelin/notebook/
chmod -R 755 /opt/incubator-zeppelin/notebook/

#Import Spark Model
cd ../Model
unzip nostradamusSVMModel.zip
cp nostradamusSVMModel /tmp/
cp DeviceLogTrainingData.csv /tmp/
sudo -u hdfs hadoop fs -chmod 777 /demo/data/
hadoop fs -mkdir /demo/data/model/
hadoop fs -mkdir /demo/data/checkpoint
hadoop fs -mkdir /demo/data/training/
hadoop fs -put /tmp/nostradamusSVMModel /demo/data/model/ 
hadoop fs -put /tmp/DeviceLogTrainingData.csv /demo/data/training/
rm -Rf /tmp/nostradamusSVMModel
rm -f /tmp/DeviceLogTrainingData.csv

#Configure Kafka
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic TechnicianEvent
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic DeviceEvents

#Start Solr and create index
service solr start
chkconfig --add solr
chkconfig solr on
sleep 10
curl "http://localhost:8983/solr/admin/cores?action=CREATE&name=settopbox&instanceDir=/opt/lucidworks-hdpsearch/solr/server/solr/settopbox&configSet=data_driven_schema_configs"

#Install and start Docker
tee /etc/yum.repos.d/docker.repo <<-'EOF'
[dockerrepo]
name=Docker Repository
baseurl=https://yum.dockerproject.org/repo/main/centos/$releasever/
enabled=1
gpgcheck=1
gpgkey=https://yum.dockerproject.org/gpg
EOF

rpm -iUvh http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
yum -y install docker-io
groupadd docker
gpasswd -a yarn docker
service docker start
chkconfig --add docker
chkconfig docker on
sudo -u hdfs hadoop fs -mkdir /user/root/
sudo -u hdfs hadoop fs -chown root:hdfs /user/root/

#Create Docker working folder
cd ../SliderConfig
mkdir /usr/hdp/docker/
mkdir /usr/hdp/docker/dockerbuild/
mkdir /usr/hdp/docker/dockerbuild/mapui
mv appConfig.json /usr/hdp/docker/dockerbuild/mapui
mv metainfo.json /usr/hdp/docker/dockerbuild/mapui
mv resources.json /usr/hdp/docker/dockerbuild/mapui

#Install NiFi Service in Ambari. Still need to log into Ambari and install the service from the console
VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
sudo git clone https://github.com/abajwa-hw/ambari-nifi-service.git   /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/NIFI
service ambari restart


#slider create mapui --template /usr/hdp/docker/dockerbuild/mapui/appConfig.json --metainfo /usr/hdp/docker/dockerbuild/mapui/metainfo.json --resources /usr/hdp/docker/dockerbuild/mapui/resources.json

#storm jar /home/storm/DeviceMonitor-0.0.1-SNAPSHOT.jar com.hortonworks.iot.topology.DeviceMonitorTopology

#spark-submit --class com.hortonworks.iot.spark.streaming.SparkNostradamus --master local[4] /home/spark/DeviceMonitorNostradamus-0.0.1-SNAPSHOT-jar-with-dependencies.jar

###### use —master yarn-client or yarn-cluster to start SparkNostradamus on Yarn (Need more RAM and CPU)