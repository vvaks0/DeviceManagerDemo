#!/bin/bash

installUtils () {
	echo "*********************************Installing WGET..."
	yum install -y wget
	
	echo "*********************************Installing Maven..."
	wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O 	/etc/yum.repos.d/epel-apache-maven.repo
	if [ $(cat /etc/system-release|grep -Po Amazon) == Amazon ]; then
		sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
	fi
	yum install -y apache-maven
	if [ $(cat /etc/system-release|grep -Po Amazon) == Amazon ]; then
		alternatives --install /usr/bin/java java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java 20000
		alternatives --install /usr/bin/javac javac /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/javac 20000
		alternatives --install /usr/bin/jar jar /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/jar 20000
		alternatives --auto java
		alternatives --auto javac
		alternatives --auto jar
		ln -s /usr/lib/jvm/java-1.8.0 /usr/lib/jvm/java
	fi
	
	echo "*********************************Installing GIT..."
	yum install -y git
	
	echo "*********************************Installing Docker..."
	echo " 				  *****************Installing Docker via Yum..."
	if [ $(cat /etc/system-release|grep -Po Amazon) == Amazon ]; then
		yum install -y docker
	else
		echo " 				  *****************Adding Docker Yum Repo..."
		tee /etc/yum.repos.d/docker.repo <<-'EOF'
		[dockerrepo]
		name=Docker Repository
		baseurl=https://yum.dockerproject.org/repo/main/centos/$releasever/
		enabled=1
		gpgcheck=1
		gpgkey=https://yum.dockerproject.org/gpg
		EOF
		rpm -iUvh http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
		yum install -y docker-io
	fi
	
	echo " 				  *****************Configuring Docker Permissions..."
	groupadd docker
	gpasswd -a yarn docker
	echo " 				  *****************Registering Docker to Start on Boot..."
	service docker start
	chkconfig --add docker
	chkconfig docker on
}

waitForAmbari () {
       	# Wait for Ambari
       	LOOPESCAPE="false"
       	until [ "$LOOPESCAPE" == true ]; do
        TASKSTATUS=$(curl -u admin:admin -I -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME | grep -Po 'OK')
        if [ "$TASKSTATUS" == OK ]; then
                LOOPESCAPE="true"
                TASKSTATUS="READY"
        else
               	AUTHSTATUS=$(curl -u admin:admin -I -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME | grep HTTP | grep -Po '( [0-9]+)'| grep -Po '([0-9]+)')
               	if [ "$AUTHSTATUS" == 403 ]; then
               	echo "THE AMBARI PASSWORD IS NOT SET TO: admin"
               	echo "RUN COMMAND: ambari-admin-password-reset, SET PASSWORD: admin"
               	exit 403
               	else
                TASKSTATUS="PENDING"
               	fi
       	fi
       	echo "Waiting for Ambari..."
        echo "Ambari Status... " $TASKSTATUS
        sleep 2
       	done
}

serviceExists () {
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"status" : ' | grep -Po '([0-9]+)')

       	if [ "$SERVICE_STATUS" == 404 ]; then
       		echo 0
       	else
       		echo 1
       	fi
}

getServiceStatus () {
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')

       	echo $SERVICE_STATUS
}

waitForService () {
       	# Ensure that Service is not in a transitional state
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')
       	sleep 2
       	echo "$SERVICE STATUS: $SERVICE_STATUS"
       	LOOPESCAPE="false"
       	if ! [[ "$SERVICE_STATUS" == STARTED || "$SERVICE_STATUS" == INSTALLED ]]; then
        until [ "$LOOPESCAPE" == true ]; do
                SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')
            if [[ "$SERVICE_STATUS" == STARTED || "$SERVICE_STATUS" == INSTALLED ]]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************$SERVICE Status: $SERVICE_STATUS"
            sleep 2
        done
       	fi
}

waitForServiceToStart () {
       	# Ensure that Service is not in a transitional state
       	SERVICE=$1
       	SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')
       	sleep 2
       	echo "$SERVICE STATUS: $SERVICE_STATUS"
       	LOOPESCAPE="false"
       	if ! [[ "$SERVICE_STATUS" == STARTED ]]; then
        	until [ "$LOOPESCAPE" == true ]; do
                SERVICE_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep '"state" :' | grep -Po '([A-Z]+)')
            if [[ "$SERVICE_STATUS" == STARTED ]]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************$SERVICE Status: $SERVICE_STATUS"
            sleep 2
        done
       	fi
}

stopService () {
       	SERVICE=$1
       	SERVICE_STATUS=$(getServiceStatus $SERVICE)
       	echo "*********************************Stopping Service $SERVICE ..."
       	if [ "$SERVICE_STATUS" == STARTED ]; then
        TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d "{\"RequestInfo\": {\"context\": \"Stop $SERVICE\"}, \"ServiceInfo\": {\"maintenance_state\" : \"OFF\", \"state\": \"INSTALLED\"}}" http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep "id" | grep -Po '([0-9]+)')

        echo "*********************************Stop $SERVICE TaskID $TASKID"
        sleep 2
        LOOPESCAPE="false"
        until [ "$LOOPESCAPE" == true ]; do
            TASKSTATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
            if [ "$TASKSTATUS" == COMPLETED ]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************Stop $SERVICE Task Status $TASKSTATUS"
            sleep 2
        done
        echo "*********************************$SERVICE Service Stopped..."
       	elif [ "$SERVICE_STATUS" == INSTALLED ]; then
       	echo "*********************************$SERVICE Service Stopped..."
       	fi
}

startService (){
       	SERVICE=$1
       	SERVICE_STATUS=$(getServiceStatus $SERVICE)
       	echo "*********************************Starting Service $SERVICE ..."
       	if [ "$SERVICE_STATUS" == INSTALLED ]; then
        TASKID=$(curl -u admin:admin -H "X-Requested-By:ambari" -i -X PUT -d "{\"RequestInfo\": {\"context\": \"Start $SERVICE\"}, \"ServiceInfo\": {\"maintenance_state\" : \"OFF\", \"state\": \"STARTED\"}}" http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE | grep "id" | grep -Po '([0-9]+)')

        echo "*********************************Start $SERVICE TaskID $TASKID"
        sleep 2
        LOOPESCAPE="false"
        until [ "$LOOPESCAPE" == true ]; do
            TASKSTATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/requests/$TASKID | grep "request_status" | grep -Po '([A-Z]+)')
            if [[ "$TASKSTATUS" == COMPLETED || "$TASKSTATUS" == FAILED ]]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************Start $SERVICE Task Status $TASKSTATUS"
            sleep 2
        done
       	elif [ "$SERVICE_STATUS" == STARTED ]; then
       	echo "*********************************$SERVICE Service Started..."
       	fi
}

getComponentStatus () {
       	SERVICE=$1
       	COMPONENT=$2
       	COMPONENT_STATUS=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/$SERVICE/components/$COMPONENT | grep '"state" :' | grep -Po '([A-Z]+)')

       	echo $COMPONENT_STATUS
}

getRegistryHost () {
       	REGISTRY_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/REGISTRY/components/REGISTRY_SERVER |grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $REGISTRY_HOST
}

getLivyHost () {
       	LIVY_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/SPARK2/components/LIVY2_SERVER |grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $LIVY_HOST
}

getHiveInteractiveServerHost () {
        HIVESERVER_INTERACTIVE_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/HIVE/components/HIVE_SERVER_INTERACTIVE|grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')

        echo $HIVESERVER_INTERACTIVE_HOST
}

getDruidBroker () {
        DRUID_BROKER=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/DRUID/components/DRUID_BROKER|grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')

        echo $DRUID_BROKER
}

getKafkaBroker () {
       	KAFKA_BROKER=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/KAFKA/components/KAFKA_BROKER |grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $KAFKA_BROKER
}

getAtlasHost () {
       	ATLAS_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/ATLAS/components/ATLAS_SERVER |grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')
       	
       	echo $ATLAS_HOST
}

getNifiHost () {
       	NIFI_HOST=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters/$CLUSTER_NAME/services/NIFI/components/NIFI_MASTER|grep "host_name"|grep -Po ': "([a-zA-Z0-9\-_!?.]+)'|grep -Po '([a-zA-Z0-9\-_!?.]+)')

       	echo $NIFI_HOST
}

captureEnvironment () {
	export NIFI_HOST=$(getNifiHost)
	export NAMENODE_HOST=$(getNameNodeHost)
	export HIVESERVER_HOST=$(getHiveServerHost)
	export HIVE_METASTORE_HOST=$(getHiveMetaStoreHost)
	export HIVE_METASTORE_URI=thrift://$HIVE_METASTORE_HOST:9083
	export ZK_HOST=$AMBARI_HOST
	export KAFKA_BROKER=$(getKafkaBroker)
	export ATLAS_HOST=$(getAtlasHost)
	export COMETD_HOST=$AMBARI_HOST
	env
	echo "export NIFI_HOST=$NIFI_HOST" >> /etc/bashrc
	echo "export NAMENODE_HOST=$NAMENODE_HOST" >> /etc/bashrc
	echo "export ZK_HOST=$ZK_HOST" >> /etc/bashrc
	echo "export KAFKA_BROKER=$KAFKA_BROKER" >> /etc/bashrc
	echo "export ATLAS_HOST=$ATLAS_HOST" >> /etc/bashrc
	echo "export HIVE_METASTORE_HOST=$HIVE_METASTORE_HOST" >> /etc/bashrc
	echo "export HIVE_METASTORE_URI=$HIVE_METASTORE_URI" >> /etc/bashrc
	echo "export COMETD_HOST=$COMETD_HOST" >> /etc/bashrc

	echo "export NIFI_HOST=$NIFI_HOST" >> ~/.bash_profile
	echo "export NAMENODE_HOST=$NAMENODE_HOST" >> ~/.bash_profile
	echo "export ZK_HOST=$ZK_HOST" >> ~/.bash_profile
	echo "export KAFKA_BROKER=$KAFKA_BROKER" >> ~/.bash_profile
	echo "export ATLAS_HOST=$ATLAS_HOST" >> ~/.bash_profile
	echo "export HIVE_METASTORE_HOST=$HIVE_METASTORE_HOST" >> ~/.bash_profile
	echo "export HIVE_METASTORE_URI=$HIVE_METASTORE_URI" >> ~/.bash_profile
	echo "export COMETD_HOST=$COMETD_HOST" >> ~/.bash_profile

	. ~/.bash_profile
}

deployTemplateToNifi () {
       	TEMPLATE_DIR=$1
       	
       	echo "*********************************Importing NIFI Template..."
       	# Import NIFI Template HDF 3.x
       	# TEMPLATE_DIR should have been passed in by the caller install process
       	sleep 1
       	TEMPLATEID=$(curl -v -F template=@"$TEMPLATE_DIR/device-manager.xml" -X POST http://$NIFI_HOST:9090/nifi-api/process-groups/root/templates/upload | grep -Po '<id>([a-z0-9-]+)' | grep -Po '>([a-z0-9-]+)' | grep -Po '([a-z0-9-]+)')
       	sleep 1

       	# Instantiate NIFI Template 3.x
       	echo "*********************************Instantiating NIFI Flow..."
       	curl -u admin:admin -i -H "Content-Type:application/json" -d "{\"templateId\":\"$TEMPLATEID\",\"originX\":100,\"originY\":100}" -X POST http://$NIFI_HOST:9090/nifi-api/process-groups/root/template-instance
       	sleep 1

       	# Rename NIFI Root Group HDF 3.x
       	echo "*********************************Renaming Nifi Root Group..."
       	ROOT_GROUP_REVISION=$(curl -X GET http://$NIFI_HOST:9090/nifi-api/process-groups/root |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')

       	sleep 1
       	ROOT_GROUP_ID=$(curl -X GET http://$NIFI_HOST:9090/nifi-api/process-groups/root|grep -Po '("component":{"id":")([0-9a-zA-z\-]+)'| grep -Po '(:"[0-9a-zA-z\-]+)'| grep -Po '([0-9a-zA-z\-]+)')

       	PAYLOAD=$(echo "{\"id\":\"$ROOT_GROUP_ID\",\"revision\":{\"version\":$ROOT_GROUP_REVISION},\"component\":{\"id\":\"$ROOT_GROUP_ID\",\"name\":\"Device-Manager\"}}")

       	sleep 1
       	curl -d $PAYLOAD  -H "Content-Type: application/json" -X PUT http://$NIFI_HOST:9090/nifi-api/process-groups/$ROOT_GROUP_ID

}

handleGroupProcessors (){
       	TARGET_GROUP=$1

       	TARGETS=($(curl -u admin:admin -i -X GET $TARGET_GROUP/processors | grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)'))
       	length=${#TARGETS[@]}
       	echo $length
       	echo ${TARGETS[0]}

       	for ((i = 0; i < $length; i++))
       	do
       		ID=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"id":"([a-zA-z0-9\-]+)'|grep -Po ':"([a-zA-z0-9\-]+)'|grep -Po '([a-zA-z0-9\-]+)'|head -1)
       		REVISION=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')
       		TYPE=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"type":"([a-zA-Z0-9\-.]+)' |grep -Po ':"([a-zA-Z0-9\-.]+)' |grep -Po '([a-zA-Z0-9\-.]+)' |head -1)
       		NAME=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"name":"([a-zA-Z0-9\-.]+)' |grep -Po ':"([a-zA-Z0-9\-.]+)' |grep -Po '([a-zA-Z0-9\-.]+)' |head -1)
       		echo "Current Processor Path: ${TARGETS[i]}"
       		echo "Current Processor Revision: $REVISION"
       		echo "Current Processor ID: $ID"
       		echo "Current Processor TYPE: $TYPE"
       		echo "Current Processor NAME: $NAME"

       		if ! [ -z $(echo $TYPE|grep "Record") ]; then
       			echo "***************************This is a Record Processor"

       			RECORD_READER=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"record-reader":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)
                RECORD_WRITER=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"record-writer":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

                echo "Record Reader: $RECORD_READER"
				echo "Record Writer: $RECORD_WRITER"

       			SCHEMA_REGISTRY=$(curl -u admin:admin -i -X GET http://$NIFI_HOST:9090/nifi-api/controller-services/$RECORD_READER |grep -Po '"schema-registry":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

       			echo "Schema Registry: $SCHEMA_REGISTRY"

       			curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$SCHEMA_REGISTRY\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$SCHEMA_REGISTRY\",\"state\":\"ENABLED\",\"properties\":{\"url\":\"http://$REGISTRY_HOST:7788/api/v1\"}}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$SCHEMA_REGISTRY

       			curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$RECORD_READER\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$RECORD_READER\",\"state\":\"ENABLED\"}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$RECORD_READER

       			curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$RECORD_WRITER\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$RECORD_WRITER\",\"state\":\"ENABLED\"}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$RECORD_WRITER

       		fi

       		if ! [ -z $(echo $TYPE|grep "MapCache") ]; then
       			echo "***************************This is a MapCache Processor"

       			MAP_CACHE_CLIENT=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"Distributed Cache Service":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

                echo "Map Cache Client Service: $MAP_CACHE_CLIENT"

       			HBASE_CLIENT_SERVICE=$(curl -u admin:admin -i -X GET http://$NIFI_HOST:9090/nifi-api/controller-services/$MAP_CACHE_CLIENT |grep -Po '"HBase Client Service":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

       			echo "HBase Client Service: $HBASE_CLIENT_SERVICE"

       			curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$HBASE_CLIENT_SERVICE\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$HBASE_CLIENT_SERVICE\",\"state\":\"ENABLED\"}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$HBASE_CLIENT_SERVICE

       			curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$MAP_CACHE_CLIENT\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$MAP_CACHE_CLIENT\",\"state\":\"ENABLED\"}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$MAP_CACHE_CLIENT
       		fi

       		if ! [ -z $(echo $TYPE|grep "HandleHttp") ]; then
                        echo "***************************This is a HandleHttpRequest or HandleHttpResponse Processor"

                        HTTP_CONTEXT=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"HTTP Context Map":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

                        echo "HTTP Context Service: $HTTP_CONTEXT"

                        curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$HTTP_CONTEXT\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$HTTP_CONTEXT\",\"state\":\"ENABLED\"}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$HTTP_CONTEXT
            fi

       		if ! [ -z $(echo $TYPE|grep "PutDruid") ]; then
                        echo "***************************This is a PutDruid Processor"

                        DRUID_CONTROLLER=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"druid_tranquility_service":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

                        echo "Druid Tranquility Service: $DRUID_CONTROLLER"

                        curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$DRUID_CONTROLLER\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$DRUID_CONTROLLER\",\"state\":\"ENABLED\",\"properties\":{\"zk_connect_string\":\"$ZK_HOST:2181\"}}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$DRUID_CONTROLLER
            fi

       		if ! [ -z $(echo $TYPE|grep "SelectHiveQL") ]; then
                        echo "***************************This is a SelectHiveQL Processor"

                        HIVE_CONNECTION_POOL=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"Hive Database Connection Pooling Service":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

                        echo "Hive Connection Pool: $HIVE_CONNECTION_POOL"

                        curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$HIVE_CONNECTION_POOL\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$HIVE_CONNECTION_POOL\",\"state\":\"ENABLED\",\"properties\":{\"hive-db-connect-url\":\"jdbc:hive2://$HIVESERVER_INTERACTIVE_HOST:10500\"}}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$HIVE_CONNECTION_POOL
            fi

       		if ! [ -z $(echo $TYPE|grep "ExecuteSparkInteractive") ]; then
                        echo "***************************This is a ExecuteSparkInteractive Processor"

                        LIVY_CONTROLLER=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"livy_controller_service":"[a-zA-Z0-9-]+'|grep -Po ':"[a-zA-Z0-9-]+'|grep -Po '[a-zA-Z0-9-]+'|head -1)

                        echo "Livy Controller Service: $LIVY_CONTROLLER"

                        curl -u admin:admin -i -H "Content-Type:application/json" -X PUT -d "{\"id\":\"$LIVY_CONTROLLER\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$LIVY_CONTROLLER\",\"state\":\"ENABLED\",\"properties\":{\"livy_host\":\"$LIVY_HOST\"}}}" http://$NIFI_HOST:9090/nifi-api/controller-services/$LIVY_CONTROLLER
            fi

       		if ! [[ -z $(echo $TYPE|grep "ConsumeKafka") || -z $(echo $TYPE|grep "PublishKafka") ]]; then
       			echo "***************************This is a Kafka Processor"
       			echo "***************************Updating Kafka Broker Porperty and Activating Processor..."
       			if ! [[ -z $(echo $TYPE|grep "ConsumeKafka") || -z $(echo $TYPE|grep "PublishKafka") ]]; then
                	PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"config\":{\"properties\":{\"bootstrap.servers\":\"$AMBARI_HOST:6667\"}},\"state\":\"RUNNING\"}}")
                fi
       		else
       			if ! [ -z $(echo $NAME|grep "SparkPrepareParameters") ]; then
                	echo "***************************This Processor Contains Parameters for Livy Spark Integration"
					PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"config\":{\"properties\":{\"druidBrokerHost\":\"$DRUID_BROKER\"}},\"state\":\"RUNNING\"}}")

            	else
       				echo "***************************Activating Processor..."
       				PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"state\":\"RUNNING\"}}")
       			fi
       		fi
       		echo "$PAYLOAD"

       		curl -u admin:admin -i -H "Content-Type:application/json" -d "${PAYLOAD}" -X PUT ${TARGETS[i]}
       	done
}

handleGroupPorts (){
       	TARGET_GROUP=$1

       	TARGETS=($(curl -u admin:admin -i -X GET $TARGET_GROUP/output-ports | grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)'))
       	length=${#TARGETS[@]}
       	echo $length
       	echo ${TARGETS[0]}

       	for ((i = 0; i < $length; i++))
       	do
       		ID=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"id":"([a-zA-z0-9\-]+)'|grep -Po ':"([a-zA-z0-9\-]+)'|grep -Po '([a-zA-z0-9\-]+)'|head -1)
       		REVISION=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')
       		TYPE=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"type":"([a-zA-Z0-9\-.]+)' |grep -Po ':"([a-zA-Z0-9\-.]+)' |grep -Po '([a-zA-Z0-9\-.]+)' |head -1)
       		echo "Current Processor Path: ${TARGETS[i]}"
       		echo "Current Processor Revision: $REVISION"
       		echo "Current Processor ID: $ID"

       		echo "***************************Activating Port ${TARGETS[i]}..."

       		PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"state\": \"RUNNING\"}}")

       		echo "PAYLOAD"
       		curl -u admin:admin -i -H "Content-Type:application/json" -d "${PAYLOAD}" -X PUT ${TARGETS[i]}
       	done
       	
       	TARGETS=($(curl -u admin:admin -i -X GET $TARGET_GROUP/input-ports | grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)'))
       	length=${#TARGETS[@]}
       	echo $length
       	echo ${TARGETS[0]}

       	for ((i = 0; i < $length; i++))
       	do
       		ID=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"id":"([a-zA-z0-9\-]+)'|grep -Po ':"([a-zA-z0-9\-]+)'|grep -Po '([a-zA-z0-9\-]+)'|head -1)
       		REVISION=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '\"version\":([0-9]+)'|grep -Po '([0-9]+)')
       		TYPE=$(curl -u admin:admin -i -X GET ${TARGETS[i]} |grep -Po '"type":"([a-zA-Z0-9\-.]+)' |grep -Po ':"([a-zA-Z0-9\-.]+)' |grep -Po '([a-zA-Z0-9\-.]+)' |head -1)
       		echo "Current Processor Path: ${TARGETS[i]}"
       		echo "Current Processor Revision: $REVISION"
       		echo "Current Processor ID: $ID"

       		echo "***************************Activating Port ${TARGETS[i]}..."

       		PAYLOAD=$(echo "{\"id\":\"$ID\",\"revision\":{\"version\":$REVISION},\"component\":{\"id\":\"$ID\",\"state\": \"RUNNING\"}}")

       		echo "PAYLOAD"
       		curl -u admin:admin -i -H "Content-Type:application/json" -d "${PAYLOAD}" -X PUT ${TARGETS[i]}
       	done
}

configureNifiTempate () {
	GROUP_TARGETS=$(curl -u admin:admin -i -X GET http://$AMBARI_HOST:9090/nifi-api/process-groups/root/process-groups | grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)')
    length=${#GROUP_TARGETS[@]}
    echo $length
    echo ${GROUP_TARGETS[0]}

    #for ((i = 0; i < $length; i++))
    for GROUP in $GROUP_TARGETS
    do
       	#CURRENT_GROUP=${GROUP_TARGETS[i]}
       	CURRENT_GROUP=$GROUP
       	echo "***********************************************************calling handle ports with group $CURRENT_GROUP"
       	handleGroupPorts $CURRENT_GROUP
       	echo "***********************************************************calling handle processors with group $CURRENT_GROUP"
       	handleGroupProcessors $CURRENT_GROUP
       	echo "***********************************************************done handle processors"
    done

    ROOT_TARGET=$(curl -u admin:admin -i -X GET http://$AMBARI_HOST:9090/nifi-api/process-groups/root| grep -Po '\"uri\":\"([a-z0-9-://.]+)' | grep -Po '(?!.*\")([a-z0-9-://.]+)')

    handleGroupPorts $ROOT_TARGET

    handleGroupProcessors $ROOT_TARGET
}

createSAMCluster() {
	#Import cluster
	export CLUSTER_ID=$(curl -H "content-type:application/json" -X POST http://$AMBARI_HOST:7777/api/v1/catalog/clusters -d '{"name":"'$CLUSTER_NAME'","description":"Demo Cluster","ambariImportUrl":"http://'$AMBARI_HOST':8080/api/v1/clusters/'$CLUSTER_NAME'"}'| grep -Po '\"id\":([0-9]+)'|grep -Po '([0-9]+)')

	#Import cluster config
	curl -H "content-type:application/json" -X POST http://$AMBARI_HOST:7777/api/v1/catalog/cluster/import/ambari -d '{"clusterId":'$CLUSTER_ID',"ambariRestApiRootUrl":"http://'$AMBARI_HOST':8080/api/v1/clusters/'$CLUSTER_NAME'","password":"admin","username":"admin"}'
}

initializeSAMNamespace () {
	#Initialize New Namespace
	export NAMESPACE_ID=$(curl -H "content-type:application/json" -X POST http://$AMBARI_HOST:7777/api/v1/catalog/namespaces -d '{"name":"dev","description":"dev","streamingEngine":"STORM"}'| grep -Po '\"id\":([0-9]+)'|grep -Po '([0-9]+)')

	#Add Services to Namespace
	curl -H "content-type:application/json" -X POST http://$AMBARI_HOST:7777/api/v1/catalog/namespaces/$NAMESPACE_ID/mapping/bulk -d '[{"clusterId":'$CLUSTER_ID',"serviceName":"STORM","namespaceId":'$NAMESPACE_ID'},{"clusterId":'$CLUSTER_ID',"serviceName":"HDFS","namespaceId":'$NAMESPACE_ID'},{"clusterId":'$CLUSTER_ID',"serviceName":"HBASE","namespaceId":'$NAMESPACE_ID'},{"clusterId":'$CLUSTER_ID',"serviceName":"KAFKA","namespaceId":'$NAMESPACE_ID'},{"clusterId":'$CLUSTER_ID',"serviceName":"DRUID","namespaceId":'$NAMESPACE_ID'},{"clusterId":'$CLUSTER_ID',"serviceName":"HDFS","namespaceId":'$NAMESPACE_ID'},{"clusterId":'$CLUSTER_ID',"serviceName":"HIVE","namespaceId":'$NAMESPACE_ID'},{"clusterId":'$CLUSTER_ID',"serviceName":"ZOOKEEPER","namespaceId":'$NAMESPACE_ID'}]'
}

uploadSAMExtensions() {
	#Import UDF and UDAF
	cd $ROOT_PATH/sam-custom-extensions/sam-custom-udf/
	mvn clean package
	mvn assembly:assembly

	curl -F udfJarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-udf/target/sam-custom-udf-0.0.5.jar -F 'udfConfig={"name":"COLLECT_LIST","displayName":"COLLECT_LIST","description":"COLLECT_LIST","type":"AGGREGATE","className":"hortonworks.hdf.sam.custom.udaf.CollectList"};type=application/json' -X POST http://$AMBARI_HOST:7777/api/v1/catalog/streams/udfs

	curl -F udfJarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-udf/target/sam-custom-udf-0.0.5.jar -F 'udfConfig={"name":"INT_TO_DOUBLE","displayName":"INT_TO_DOUBLE","description":"Int to Double","type":"FUNCTION","className":"hortonworks.hdf.sam.custom.udf.datatype.conversion.IntToDouble"};type=application/json' -X POST http://$AMBARI_HOST:7777/api/v1/catalog/streams/udfs

	curl -F udfJarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-udf/target/sam-custom-udf-0.0.5.jar -F 'udfConfig={"name":"INT_TO_STRING","displayName":"INT_TO_STRING","description":"INT_TO_STRING","type":"FUNCTION","className":"hortonworks.hdf.sam.custom.udf.datatype.conversion.IntToString"};type=application/json' -X POST http://$AMBARI_HOST:7777/api/v1/catalog/streams/udfs

	#Import Custom Processors
	cd $ROOT_PATH/sam-custom-extensions/sam-custom-processor
	mvn clean package -DskipTests
	mvn assembly:assembly -DskipTests

	curl -sS -X POST -i -F jarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor/target/sam-custom-processor-0.0.5-jar-with-dependencies.jar http://$AMBARI_HOST:7777/api/v1/catalog/streams/componentbundles/PROCESSOR/custom -F customProcessorInfo=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor/src/main/resources/phoenix-enrich.json

	cd $ROOT_PATH/sam-custom-extensions/sam-custom-processor-phoenix-upsert
	mvn clean package -DskipTests
	mvn assembly:assembly -DskipTests

	curl -sS -X POST -i -F jarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-phoenix-upsert/target/sam-custom-processor-phoenix-upsert-0.0.1-SNAPSHOT-jar-with-dependencies.jar  http://$AMBARI_HOST:7777/api/v1/catalog/streams/componentbundles/PROCESSOR/custom -F customProcessorInfo=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-phoenix-upsert/src/main/resources/phoenix-upsert.json

	cd $ROOT_PATH/sam-custom-extensions/sam-custom-sink
	mvn clean package -DskipTests
	mvn assembly:assembly -DskipTests

	curl -sS -X POST -i -F jarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-sink/target/sam-custom-sink-0.0.5-jar-with-dependencies.jar http://$AMBARI_HOST:7777/api/v1/catalog/streams/componentbundles/PROCESSOR/custom -F customProcessorInfo=@$ROOT_PATH/sam-custom-extensions/sam-custom-sink/src/main/resources/cometd-sink.json

	cd $ROOT_PATH/sam-custom-extensions/sam-custom-processor-explode-collection
	mvn clean package -DskipTests
	mvn assembly:assembly -DskipTests

	curl -sS -X POST -i -F jarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-explode-collection/target/sam-custom-processor-explode-collection-0.0.1-SNAPSHOT-jar-with-dependencies.jar  http://$AMBARI_HOST:7777/api/v1/catalog/streams/componentbundles/PROCESSOR/custom -F customProcessorInfo=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-explode-collection/src/main/resources/explode-collection.json

	cd $ROOT_PATH/sam-custom-extensions/sam-custom-processor-inject-field
mvn clean package -DskipTests
mvn assembly:assembly -DskipTests

	curl -sS -X POST -i -F jarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-inject-field/target/sam-custom-processor-inject-field-0.0.1-SNAPSHOT.jar  http://$AMBARI_HOST:7777/api/v1/catalog/streams/componentbundles/PROCESSOR/custom -F customProcessorInfo=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-inject-field/src/main/resources/inject-field.json

	cd $ROOT_PATH/sam-custom-extensions/sam-custom-processor-routetech
	mvn clean package -DskipTests
	mvn assembly:assembly -DskipTests

	curl -sS -X POST -i -F jarFile=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-routetech/target/sam-custom-processor-routetech-0.0.1-SNAPSHOT-jar-with-dependencies.jar  http://$AMBARI_HOST:7777/api/v1/catalog/streams/componentbundles/PROCESSOR/custom -F customProcessorInfo=@$ROOT_PATH/sam-custom-extensions/sam-custom-processor-routetech/src/main/resources/recommend-tech.json
	
	cd $ROOT_PATH
}

importPMMLModel () {
	MODEL_DIR=$1
	#Import PMML Model - Need to git clone PMML file from somewhere
	curl -sS -i -F pmmlFile=@$MODEL_DIR/device_manager_svm.xml -F 'modelInfo={"name":"device_monitor_svm","namespace":"ml_model","uploadedFileName":"device_manager_svm.xml"};type=text/json' -X POST http://$AMBARI_HOST:7777/api/v1/catalog/ml/models
}

importSAMTopology () {
	SAM_DIR=$1
	#Import Topology
	sed -r -i 's;\{\{HOST1\}\};'$AMBARI_HOST';g' $SAM_DIR/device-manager.json
	sed -r -i 's;\{\{CLUSTERNAME\}\};'$CLUSTER_NAME';g' $SAM_DIR/device-manager.json
 
	export TOPOLOGY_ID=$(curl -F file=@$SAM_DIR/device-manager.json -F 'topologyName=device-manager' -F 'namespaceId='$NAMESPACE_ID -X POST http://$AMBARI_HOST:7777/api/v1/catalog/topologies/actions/import| grep -Po '\"topologyId\":([0-9]+)'|grep -Po '([0-9]+)')

}

deploySAMTopology () {
	#Deploy Topology
	curl -X POST http://$AMBARI_HOST:7777/api/v1/catalog/topologies/$TOPOLOGY_ID/versions/$TOPOLOGY_ID/actions/deploy
	
	#Poll Deployment State until deployment completes or fails
	TOPOLOGY_STATUS=$(curl -X GET http://$AMBARI_HOST:7777/api/v1/catalog/topologies/$TOPOLOGY_ID/deploymentstate | grep -Po '"name":"([A-Z_]+)'| grep -Po '([A-Z_]+)')
    sleep 2
    echo "TOPOLOGY STATUS: $TOPOLOGY_STATUS"
    LOOPESCAPE="false"
    if ! [[ "$TOPOLOGY_STATUS" == TOPOLOGY_STATE_DEPLOYED || "$TOPOLOGY_STATUS" == TOPOLOGY_STATE_DEPLOYMENT_FAILED ]]; then
    	until [ "$LOOPESCAPE" == true ]; do
            TOPOLOGY_STATUS=$(curl -X GET http://$AMBARI_HOST:7777/api/v1/catalog/topologies/$TOPOLOGY_ID/deploymentstate | grep -Po '"name":"([A-Z_]+)'| grep -Po '([A-Z_]+)')
            if [[ "$TOPOLOGY_STATUS" == TOPOLOGY_STATE_DEPLOYED || "$TOPOLOGY_STATUS" == TOPOLOGY_STATE_DEPLOYMENT_FAILED ]]; then
                LOOPESCAPE="true"
            fi
            echo "*********************************TOPOLOGY STATUS: $TOPOLOGY_STATUS"
            sleep 2
        done
    fi
}

pushSchemasToRegistry (){

#technician_incoming
PAYLOAD="{\"name\":\"technician_incoming\",\"type\":\"avro\",\"schemaGroup\":\"technician\",\"description\":\"technician_incoming\",\"evolve\":true,\"compatibility\":\"BACKWARD\"}"

	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas
	
	PAYLOAD="{\"description\":\"technician status\",\"schemaText\":\"{\\n   \\\"type\\\" : \\\"record\\\",\\n   \\\"namespace\\\" : \\\"com.hortonworks\\\",\\n   \\\"name\\\" : \\\"technician_status\\\",\\n   \\\"fields\\\" : [\\n\\t  { \\\"name\\\" : \\\"technician_id\\\" , \\\"type\\\" : \\\"string\\\" },\\n      { \\\"name\\\" : \\\"longitude\\\" , \\\"type\\\" : \\\"double\\\" },\\n      { \\\"name\\\" : \\\"latitude\\\" , \\\"type\\\" : \\\"double\\\" },\\n      { \\\"name\\\" : \\\"status\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"ip_address\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"port\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"event_type\\\" , \\\"type\\\" : \\\"string\\\"}\\n\\t]\\n}\"}"

	echo $PAYLOAD
	
	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas/technician_incoming/versions
	

#technician_status
		PAYLOAD="{\"name\":\"technician_status\",\"type\":\"avro\",\"schemaGroup\":\"technician\",\"description\":\"technician_status\",\"evolve\":true,\"compatibility\":\"BACKWARD\"}"
	
	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas
		
	PAYLOAD="{\"description\":\"technician status\",\"schemaText\":\"{\\n   \\\"type\\\" : \\\"record\\\",\\n   \\\"namespace\\\" : \\\"com.hortonworks\\\",\\n   \\\"name\\\" : \\\"technician_status\\\",\\n   \\\"fields\\\" : [\\n\\t  { \\\"name\\\" : \\\"technician_id\\\" , \\\"type\\\" : \\\"string\\\" },\\n      { \\\"name\\\" : \\\"longitude\\\" , \\\"type\\\" : \\\"double\\\" },\\n      { \\\"name\\\" : \\\"latitude\\\" , \\\"type\\\" : \\\"double\\\" },\\n      { \\\"name\\\" : \\\"status\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"ip_address\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"port\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"event_type\\\" , \\\"type\\\" : \\\"string\\\"}\\n\\t]\\n}\"}"

	echo $PAYLOAD

	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas/technician_status/versions
	
#technician_destination
	PAYLOAD="{\"name\":\"technician_destination\",\"type\":\"avro\",\"schemaGroup\":\"technician\",\"description\":\"technician_destination\",\"evolve\":true,\"compatibility\":\"BACKWARD\"}"
	
	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas
	
	PAYLOAD="{\"description\":\"technician destination\",\"schemaText\":\"{\\n   \\\"type\\\" : \\\"record\\\",\\n   \\\"namespace\\\" : \\\"com.hortonworks\\\",\\n   \\\"name\\\" : \\\"technician_destination\\\",\\n   \\\"fields\\\" : [\\n\\t  { \\\"name\\\" : \\\"technician_id\\\" , \\\"type\\\" : \\\"string\\\" },\\n      { \\\"name\\\" : \\\"longitude\\\" , \\\"type\\\" : \\\"double\\\" },\\n      { \\\"name\\\" : \\\"latitude\\\" , \\\"type\\\" : \\\"double\\\" },\\n      { \\\"name\\\" : \\\"status\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"ip_address\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"port\\\" , \\\"type\\\" : \\\"string\\\"},\\n      { \\\"name\\\" : \\\"event_type\\\" , \\\"type\\\": [\\\"null\\\", \\\"string\\\"]},\\n      { \\\"name\\\" : \\\"destination_longitude\\\" , \\\"type\\\" : \\\"double\\\"},\\n      { \\\"name\\\" : \\\"destination_latitude\\\" , \\\"type\\\" : \\\"double\\\"}\\n\\t]\\n}\"}"

	echo $PAYLOAD
	
	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas/technician_destination/versions

#stb_incoming
	
PAYLOAD="{\"name\":\"stb_incoming\",\"type\":\"avro\",\"schemaGroup\":\"device\",\"description\":\"stb_incoming\",\"evolve\":true,\"compatibility\":\"BACKWARD\"}"
	
	curl -u admin:admin -i -H "content-type: application/json" -d $PAYLOAD -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas
	
	PAYLOAD="{\"description\":\"stb incoming\",\"schemaText\":\"{\\n \\\"type\\\": \\\"record\\\",\\n \\\"namespace\\\": \\\"com.hortonworks\\\",\\n \\\"name\\\": \\\"stb_incoming\\\",\\n \\\"fields\\\": [\\n  {\\n   \\\"name\\\": \\\"serialNumber\\\",\\n   \\\"type\\\": \\\"string\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"state\\\",\\n   \\\"type\\\": \\\"string\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"status\\\",\\n   \\\"type\\\": \\\"string\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"signalStrength\\\",\\n   \\\"type\\\": \\\"int\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"internalTemp\\\",\\n   \\\"type\\\": \\\"int\\\"\\n  }\\n ]\\n}\"}"

	echo $PAYLOAD
	
	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas/stb_incoming/versions
	
#stb_status	

PAYLOAD="{\"name\":\"stb_status\",\"type\":\"avro\",\"schemaGroup\":\"device\",\"description\":\"stb_status\",\"evolve\":true,\"compatibility\":\"BACKWARD\"}"
	
	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas
	
	PAYLOAD="{\"description\":\"set top box status\",\"schemaText\":\"{\\n \\\"type\\\": \\\"record\\\",\\n \\\"namespace\\\": \\\"com.hortonworks\\\",\\n \\\"name\\\": \\\"stb_details\\\",\\n \\\"fields\\\": [\\n  {\\n   \\\"name\\\": \\\"serial_number\\\",\\n   \\\"type\\\": \\\"string\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"state\\\",\\n   \\\"type\\\": \\\"string\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"status\\\",\\n   \\\"type\\\": \\\"string\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"signal_strength\\\",\\n   \\\"type\\\": \\\"int\\\"\\n  },\\n  {\\n   \\\"name\\\": \\\"internal_temp\\\",\\n   \\\"type\\\": \\\"int\\\"\\n  }\\n ]\\n}\"}"

	echo $PAYLOAD
	
	curl -u admin:admin -i -H "content-type: application/json" -d "$PAYLOAD" -X POST http://$AMBARI_HOST:7788/api/v1/schemaregistry/schemas/stb_status/versions

	
}

deployContainers (){
	APP_DIR=$1
	#cd APP_DIR/Cometd
	#mvn clean package
	#mvn docker:build
	
	cd APP_DIR/Map_UI
	mvn clean package
	mvn docker:build
	
	docker pull vvaks/cometd
	docker run -d -p 8099:8091 vvaks/cometd
	docker run -d -e MAP_API_KEY=$GOOGLE_API_KEY -e ZK_HOST=$ZK_HOST -e COMETD_HOST=$COMETD_HOST -e COMETD_PORT=8099 -p 8098:8090 vvaks/mapui

}

enablePhoenix () {
	echo "*********************************Installing Phoenix Binaries..."
	yum install -y phoenix
	echo "*********************************Enabling Phoenix..."
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site phoenix.functions.allowUserDefinedFunctions true
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.defaults.for.version.skip true
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.regionserver.wal.codec org.apache.hadoop.hbase.regionserver.wal.IndexedWALEditCodec
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.region.server.rpc.scheduler.factory.class org.apache.hadoop.hbase.ipc.PhoenixRpcSchedulerFactory
	sleep 1
	/var/lib/ambari-server/resources/scripts/configs.sh set $AMBARI_HOST $CLUSTER_NAME hbase-site hbase.rpc.controllerfactory.class org.apache.hadoop.hbase.ipc.controller.ServerRpcControllerFactory
}

createHbaseTables () {
	#Create Hbase Tables
	echo "create 'device_events','0'" | hbase shell
	echo "create 'technician_location','0'" | hbase shell
}
createHbaseTables
createPhoenixTables () {
	#Create Phoenix Tables
	tee create_device_details.sql <<-'EOF'
CREATE TABLE IF NOT EXISTS DEVICEDETAILS (SERIALNUMBER VARCHAR PRIMARY KEY, DEVICEMODEL VARCHAR, LATITUDE VARCHAR, LONGITUDE VARCHAR, IPADDRESS VARCHAR, PORT VARCHAR, ASSIGNEDTECH VARCHAR);

UPSERT INTO DEVICEDETAILS VALUES('1000','Motorolla','39.951694','-75.144596','192.168.56.1','8085','None');

UPSERT INTO DEVICEDETAILS VALUES('2000','Motorolla','39.970279','-75.175152','192.168.56.1','8087','None');

UPSERT INTO DEVICEDETAILS VALUES('3000','Motorolla','39.948174','-75.170689','192.168.56.1','8089','None');
EOF

	/usr/hdp/current/phoenix-client/bin/sqlline.py $ZK_HOST:2181:/hbase-unsecure create_device_details.sql
}

createKafkaTopics () {
	/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --zookeeper $ZK_HOST:2181 --create --topic stb_status --partitions 1 --replication-factor 1

/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --zookeeper $ZK_HOST:2181 --create --topic technician_status --partitions 1 --replication-factor 1

}

export ROOT_PATH=$1
echo "*********************************ROOT PATH IS: $ROOT_PATH"

export GOOGLE_API_KEY=$2

export AMBARI_HOST=$(hostname -f)
echo "*********************************AMABRI HOST IS: $AMBARI_HOST"

export CLUSTER_NAME=$(curl -u admin:admin -X GET http://$AMBARI_HOST:8080/api/v1/clusters |grep cluster_name|grep -Po ': "(.+)'|grep -Po '[a-zA-Z0-9\-_!?.]+')

if [[ -z $CLUSTER_NAME ]]; then
        echo "Could not connect to Ambari Server. Please run the install script on the same host where Ambari Server is installed."
        exit 1
else
       	echo "*********************************CLUSTER NAME IS: $CLUSTER_NAME"
fi

export VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
export INTVERSION=$(echo $VERSION*10 | bc | grep -Po '([0-9][0-9])')
echo "*********************************HDP VERSION IS: $VERSION"

#git clone https://github.com/vakshorton/sam-custom-extensions
#git clone https://github.com/vakshorton/DeviceManagerDemo

echo "********************************* Enabling Phoenix"
enablePhoenix
echo "********************************* Restarting Hbase"
stopService HBASE
startService HBASE
echo "********************************* Capturing Service Endpoint in the Environment"
captureEnvironment
echo "********************************* Creating Hbase Tables"
createHbaseTables
echo "********************************* Creating Phoenix Tables"
createPhoenixTables
echo "********************************* Creating Kafka Topics"
createKafkaTopics
echo "********************************* Deploying UI and HTTP Pub/Sub containers"
deployContainers $ROOT_PATH/DeviceManagerDemo
echo "********************************* Registering Schemas"
pushSchemasToRegistry
echo "********************************* Deploying Nifi Template"
deployTemplateToNifi $ROOT_PATH/DeviceManagerDemo/Nifi/template
echo "********************************* Configuring Nifi Template"
configureNifiTempate
echo "********************************* Initializing SAM Namespace"
initializeSAMNamespace
echo "********************************* Uploading SAM Extensions"
uploadSAMExtensions
echo "********************************* Import PMML Model to SAM"
importPMMLModel $ROOT_PATH/DeviceManagerDemo/Model
echo "********************************* Import SAM Template"
importSAMTopology $ROOT_PATH/DeviceManagerDemo/SAM/template
echo "********************************* Deploy SAM Topology"
deploySAMTopology
