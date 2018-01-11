package org.jmdata.manager.srteem.zkstormkafka;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy.TimeUnit;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.kafka.BrokerHosts;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.kafka.StringScheme;
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

public class DistributeLogTopology {
    
     public static class KafkaLineFilterBolt extends BaseRichBolt {

          private static final Log LOG = LogFactory.getLog(KafkaLineFilterBolt.class);
          private static final long serialVersionUID = -5207232012035109026L;
          private OutputCollector collector;
         

          public void prepare(Map stormConf, TopologyContext context,
                    OutputCollector collector) {
               this.collector = collector;              
          }


          public void execute(Tuple input) {
               String line = input.getString(0).trim();
               String escapeLine = StringEscapeUtils.escapeJava(line);
               if(!line.isEmpty()&& escapeLine.contains("bi-action-type")) {           	             	                     
                    LOG.info("EMIT[splitter -> counter] " + line);
                    collector.emit(input, new Values(escapeLine, escapeLine.length()));
                    collector.ack(input);
               }
              
          }

          public void declareOutputFields(OutputFieldsDeclarer declarer) {
               declarer.declare(new Fields("line", "len"));         
          }
         
     }
    


     public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException, InterruptedException, AuthorizationException {

          // Configure Kafka
          String zks = "192.168.1.52:2181,192.168.1.51:2181,192.168.50:2182";        
          String topic = "flume_kafka_channel_topic";
          String zkRoot = "/storm"; // default zookeeper root configuration for storm
          String id = "word";
          BrokerHosts brokerHosts = new ZkHosts(zks);
          SpoutConfig spoutConf = new SpoutConfig(brokerHosts, topic, zkRoot, id);
          spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());
          //spoutConf.forceFromStart = false;
          spoutConf.zkServers = Arrays.asList(new String[] {"192.168.1.141", "192.168.1.145"});
          spoutConf.zkPort = 2181;
         
          // Configure HDFS bolt
          RecordFormat format = new DelimitedRecordFormat()
                  .withFieldDelimiter("||"); // use "\t" instead of "," for field delimiter
          SyncPolicy syncPolicy = new CountSyncPolicy(1000); // sync the filesystem after every 1k tuples
          FileRotationPolicy rotationPolicy = new TimedRotationPolicy(1.0f, TimeUnit.DAYS); // rotate files
          FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                  .withPath("/jmhdfs/log/storm/").withPrefix("app_").withExtension(".log"); // set file name format
          HdfsBolt hdfsBolt = new HdfsBolt()
                  .withFsUrl("hdfs://192.168.1.141:9000")
                  .withFileNameFormat(fileNameFormat)
                  .withRecordFormat(format)
                  .withRotationPolicy(rotationPolicy)
                  .withSyncPolicy(syncPolicy);
         
          // configure & build topology
          TopologyBuilder builder = new TopologyBuilder();
          builder.setSpout("kafka-reader", new KafkaSpout(spoutConf), 3);
          builder.setBolt("line-filter", new KafkaLineFilterBolt(), 3).shuffleGrouping("kafka-reader");        
          builder.setBolt("hdfs-bolt", hdfsBolt, 3).shuffleGrouping("line-filter");
          // submit topology
          Config conf = new Config();
          Map<String, String> map = new HashMap<String, String>();
          map.put("metadata.broker.list", "192.168.1.50:9000,192.168.1.51:9092,192.168.1.52:9092");
          map.put("serializer.class", "kafka.serializer.StringEncoder");
          conf.put("kafka.broker.properties", map);
          conf.put("topic", topic);
          String name = DistributeLogTopology.class.getSimpleName();
          
           if (args != null && args.length > 0) {
               conf.put(Config.NIMBUS_HOST, "bds1");
               conf.setNumWorkers(3);           
               StormSubmitter.submitTopologyWithProgressBar(args[0], conf, builder.createTopology());
          } else {
               conf.setMaxTaskParallelism(3);
               LocalCluster cluster = new LocalCluster();
               cluster.submitTopology(name, conf, builder.createTopology());
               Thread.sleep(60000);
               cluster.shutdown();
          }
     }

}