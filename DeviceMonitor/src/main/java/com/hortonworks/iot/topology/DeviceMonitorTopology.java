package com.hortonworks.iot.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.SimpleHBaseMapper;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hive.bolt.HiveBolt;
import org.apache.storm.hive.bolt.mapper.DelimitedRecordHiveMapper;
import org.apache.storm.hive.common.HiveOptions;

import com.hortonworks.bolts.EnrichDeviceStatus;
import com.hortonworks.bolts.IncidentDetector;
import com.hortonworks.bolts.PersistTechnicianLocation;
import com.hortonworks.bolts.PrintDeviceAlert;
import com.hortonworks.bolts.PrintDeviceStatus;
import com.hortonworks.bolts.PublishAlert;
import com.hortonworks.bolts.PublishDeviceStatus;
import com.hortonworks.bolts.PublishTechnicianLocation;
import com.hortonworks.bolts.RecommendTechnician;
import com.hortonworks.bolts.RouteTechnician;

import com.hortonworks.util.Constants;
import com.hortonworks.util.DeviceEventJSONScheme;
import com.hortonworks.util.TechnicianEventJSONScheme;


/*
import backtype.storm.StormSubmitter;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.KeyValueSchemeAsMultiScheme;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
*/

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.BrokerHosts;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.kafka.KeyValueSchemeAsMultiScheme;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

public class DeviceMonitorTopology {   
	static String topologyName = "DeviceMonitorTopology";
    public static void main(String[] args) {
     TopologyBuilder builder = new TopologyBuilder();
     Constants constants = new Constants();
     String hostClusterName = null; 
     String deviceLogTable = "device_status_log";
     if(args[0] != null){
     	hostClusterName = args[0];
    	topologyName = "DeviceMonitorTopology-"+hostClusterName;
    	deviceLogTable = "device_status_log_"+hostClusterName;
     }
     // Use pipe as record boundary
  	  RecordFormat format = new DelimitedRecordFormat().withFieldDelimiter(",");

  	  //Synchronize data buffer with the filesystem every 1000 tuples
  	  SyncPolicy syncPolicy = new CountSyncPolicy(1000);

  	  // Rotate data files when they reach five MB
  	  FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(5.0f, Units.MB);

  	  // Use default, Storm-generated file names
  	  FileNameFormat deviceLogFileNameFormat = new DefaultFileNameFormat().withPath(constants.getHivePath());
  	  HdfsBolt deviceLogHdfsBolt = new HdfsBolt()
  		     .withFsUrl(constants.getNameNodeUrl())
  		     .withFileNameFormat(deviceLogFileNameFormat)
  		     .withRecordFormat(format)
  		     .withRotationPolicy(rotationPolicy)
  		     .withSyncPolicy(syncPolicy);
  	System.out.println("********************** Starting Topology.......");
  	System.out.println("********************** Name Node Url: " + constants.getNameNodeUrl());
  	System.out.println("********************** Zookeeper Host: " + constants.getZkHost());
    System.out.println("********************** Zookeeper Port: " + constants.getZkPort());
    System.out.println("********************** Zookeeper ConnString: " + constants.getZkConnString());
    System.out.println("********************** Zookeeper Kafka Path: " + constants.getZkKafkaPath());
    System.out.println("********************** Zookeeper HBase Path: " + constants.getZkHBasePath());
    System.out.println("********************** Atlas Host: " + constants.getAtlasHost());
    System.out.println("********************** Atlas Port: " + constants.getAtlasPort());
    System.out.println("********************** Metastore URI: " + constants.getHiveMetaStoreURI());
    System.out.println("********************** Cometd URI: " + constants.getPubSubUrl());
  	  
      Config conf = new Config(); 
      //BrokerHosts hosts = new ZkHosts(Constants.zkConnString);
      BrokerHosts hosts = new ZkHosts(constants.getZkConnString(), constants.getZkKafkaPath());
      
      SpoutConfig deviceKafkaSpoutConfig = new SpoutConfig(hosts, constants.getDeviceEventsTopicName(), constants.getZkKafkaPath(), UUID.randomUUID().toString());
      deviceKafkaSpoutConfig.scheme = new KeyValueSchemeAsMultiScheme(new DeviceEventJSONScheme());
      deviceKafkaSpoutConfig.ignoreZkOffsets = true;
      deviceKafkaSpoutConfig.useStartOffsetTimeIfOffsetOutOfRange = true;
      deviceKafkaSpoutConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
      KafkaSpout deviceKafkaSpout = new KafkaSpout(deviceKafkaSpoutConfig); 
     
      SpoutConfig technicianKafkaSpoutConfig = new SpoutConfig(hosts, constants.getTechnicianEventsTopicName(), constants.getZkKafkaPath(), UUID.randomUUID().toString());
      technicianKafkaSpoutConfig.scheme = new KeyValueSchemeAsMultiScheme(new TechnicianEventJSONScheme());
      technicianKafkaSpoutConfig.ignoreZkOffsets = true;
      technicianKafkaSpoutConfig.useStartOffsetTimeIfOffsetOutOfRange = true;
      technicianKafkaSpoutConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
      KafkaSpout technicianKafkaSpout = new KafkaSpout(technicianKafkaSpoutConfig);
      
      Map<String, Object> hbConf = new HashMap<String, Object>();
      hbConf.put("hbase.rootdir", constants.getNameNodeUrl() + constants.getHbasePath());
      hbConf.put("hbase.zookeeper.quorum", constants.getZkHost());
	  hbConf.put("hbase.zookeeper.property.clientPort", constants.getZkPort());
      hbConf.put("zookeeper.znode.parent", constants.getZkHBasePath());
      conf.put("hbase.conf", hbConf);
      conf.put("hbase.rootdir", constants.getNameNodeUrl() + constants.getHbasePath());
      
      SimpleHBaseMapper deviceStatusLogMapper = new SimpleHBaseMapper()
              .withRowKeyField("serialNumber")
              .withColumnFields(new Fields("deviceModel", "status", "state", "internalTemp", "signalStrength"))
              .withColumnFamily("deviceStatus");
      
      DelimitedRecordHiveMapper deviceStatusLogHiveMapper = new DelimitedRecordHiveMapper()
    		  .withColumnFields(new Fields("deviceModel", "status", "state", "internalTemp", "signalStrength"));
    		  //.withPartitionFields(new Fields("accounttype"));
    		 
      HiveOptions deviceStatusLogHiveOptions = new HiveOptions(constants.getHiveMetaStoreURI(),
    				 							constants.getHiveDbName(),
    				 							deviceLogTable,
    				 							deviceStatusLogHiveMapper);
      
      //HBaseBolt hbasePersistTechnicianLocation = new HBaseBolt("TechnicianEvents", technicianLocationMapper).withConfigKey("hbase.conf");
  	  //builder.setSpout("DeviceSpout", new DeviceSpout());
      builder.setSpout("DeviceKafkaSpout", deviceKafkaSpout);
      builder.setBolt("EnrichDeviceStatus", new EnrichDeviceStatus(), 1).shuffleGrouping("DeviceKafkaSpout");      
      builder.setBolt("PublishDeviceStatus", new PublishDeviceStatus(), 1).shuffleGrouping("EnrichDeviceStatus");
      builder.setBolt("DetectIncident", new IncidentDetector(), 1).shuffleGrouping("EnrichDeviceStatus");
      builder.setBolt("PrintDeviceStatus", new PrintDeviceStatus(), 1).shuffleGrouping("DetectIncident", "DeviceStatusStream");
      builder.setBolt("PrintDeviceAlert", new PrintDeviceAlert(), 1).shuffleGrouping("DetectIncident", "DeviceAlertStream");
      builder.setBolt("DeviceLogHDFSBolt", deviceLogHdfsBolt, 1).shuffleGrouping("DetectIncident", "DeviceStatusLogStream");
      builder.setBolt("DeviceLogHBaseBolt", new HBaseBolt("DeviceStatusLog", deviceStatusLogMapper).withConfigKey("hbase.conf"), 1).shuffleGrouping("DetectIncident", "DeviceStatusLogNullStream");
      builder.setBolt("DeviceLogHiveBolt", new HiveBolt(deviceStatusLogHiveOptions),1).shuffleGrouping("DetectIncident", "DeviceStatusLogNullStream");
      builder.setBolt("PublishAlert", new PublishAlert(), 1).shuffleGrouping("PrintDeviceAlert");
      builder.setBolt("RecommendTechnician", new RecommendTechnician(), 1).shuffleGrouping("PrintDeviceAlert");
      builder.setBolt("RouteTechnician", new RouteTechnician(), 1).shuffleGrouping("RecommendTechnician");
      //builder.setBolt("persist", printerHdfsBolt).shuffleGrouping("print");
      
      builder.setSpout("TechnicianKafkaSpout", technicianKafkaSpout);
      //builder.setBolt("PersistTechnicianLocation", hbasePersistTechnicianLocation).shuffleGrouping("TechnicianKafkaSpout");       
      builder.setBolt("PersistTechnicianLocation", new PersistTechnicianLocation(), 1).shuffleGrouping("TechnicianKafkaSpout");
      builder.setBolt("PublishTechnicianLocation", new PublishTechnicianLocation(), 1).shuffleGrouping("PersistTechnicianLocation");
      
      conf.setNumWorkers(1);
      conf.setMaxSpoutPending(5000);
      conf.setMaxTaskParallelism(1);
      
      //submitToLocal(builder, conf);
      submitToCluster(builder, conf);
 }
 
 public static void submitToLocal(TopologyBuilder builder, Config conf){
	 LocalCluster cluster = new LocalCluster();
	 cluster.submitTopology(topologyName, conf, builder.createTopology()); 
 }
 
 public static void submitToCluster(TopologyBuilder builder, Config conf){
	 try {
			StormSubmitter.submitTopology(topologyName, conf, builder.createTopology());
	      } catch (AlreadyAliveException e) {
			e.printStackTrace();
	      } catch (InvalidTopologyException e) {
			e.printStackTrace();
	      } catch (AuthorizationException e) {
			e.printStackTrace();
	      }
    }
}
