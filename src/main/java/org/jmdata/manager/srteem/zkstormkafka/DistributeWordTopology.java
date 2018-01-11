package org.jmdata.manager.srteem.zkstormkafka;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

public class DistributeWordTopology {
    
     public static class KafkaWordToUpperCase extends BaseRichBolt {

          private static final Log LOG = LogFactory.getLog(KafkaWordToUpperCase.class);
          private static final long serialVersionUID = -5207232012035109026L;
          private OutputCollector collector;
         
          public void prepare(Map stormConf, TopologyContext context,
                    OutputCollector collector) {
               this.collector = collector;              
          }

          public void execute(Tuple input) {
               String line = input.getString(0).trim();
               LOG.info("RECV[kafka -> splitter] " + line);
               if(!line.isEmpty()) {
                    String upperLine = line.toUpperCase();
                    LOG.info("EMIT[splitter -> counter] " + upperLine);
                    collector.emit(input, new Values(upperLine, upperLine.length()));
               }
               collector.ack(input);
          }

          public void declareOutputFields(OutputFieldsDeclarer declarer) {
               declarer.declare(new Fields("line", "len"));         
          }
         
     }
    
     public static class RealtimeBolt extends BaseRichBolt {

          private static final Log LOG = LogFactory.getLog(KafkaWordToUpperCase.class);
          private static final long serialVersionUID = -4115132557403913367L;
          private OutputCollector collector;
         
          public void prepare(Map stormConf, TopologyContext context,
                    OutputCollector collector) {
               this.collector = collector;              
          }

          public void execute(Tuple input) {
               String line = input.getString(0).trim();
               LOG.info("REALTIME: " + line);
               collector.ack(input);
          }

          public void declareOutputFields(OutputFieldsDeclarer declarer) {
              
          }

     }

     public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException, InterruptedException, AuthorizationException {

          // Configure Kafka  
          String zks = "jmnb:2181,jmnd:2181,jmne:2181/kafka/brokers/";
          String topic = "myjmdata";
          String zkRoot = "/kafka"; // default zookeeper root configuration for storm
          String id = "worldCounts";
          BrokerHosts brokerHosts = new ZkHosts(zks);
          SpoutConfig spoutConf = new SpoutConfig(brokerHosts, topic, zkRoot, id);
          spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());
          //spoutConf.forceFromStart = false;
          spoutConf.zkServers = Arrays.asList(new String[] {"jmnb", "jmnd", "jmne"});
          spoutConf.zkPort = 2181;
         
          // Configure HDFS bolt
          RecordFormat format = new DelimitedRecordFormat().withFieldDelimiter("\t"); // use "\t" instead of "," for field delimiter
          SyncPolicy syncPolicy = new CountSyncPolicy(1000); // sync the filesystem after every 1k tuples
          FileRotationPolicy rotationPolicy = new TimedRotationPolicy(1.0f, TimeUnit.MINUTES); // rotate files
          FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                  .withPath("/kafka/").withPrefix("app_").withExtension(".log"); // set file name format
          HdfsBolt hdfsBolt = new HdfsBolt()
                  .withFsUrl("hdfs://jmnb:9000")
                  .withFileNameFormat(fileNameFormat)
                  .withRecordFormat(format)
                  .withRotationPolicy(rotationPolicy)
                  .withSyncPolicy(syncPolicy);
         
          // configure & build topology
          TopologyBuilder builder = new TopologyBuilder();
          builder.setSpout("kafka-reader", new KafkaSpout(spoutConf), 5);
          builder.setBolt("to-upper", new KafkaWordToUpperCase(), 3).shuffleGrouping("kafka-reader");
          builder.setBolt("hdfs-bolt", hdfsBolt, 2).shuffleGrouping("to-upper");
          builder.setBolt("realtime", new RealtimeBolt(), 2).shuffleGrouping("to-upper");
         
          // submit topology
          Config conf = new Config();
          String name = DistributeWordTopology.class.getSimpleName();
          args  =new String []{"jmnb"};
          String jarstr ="C:\\myhome\\usr\\workspace\\jmms\\jmdata-libs\\jmdata-bigdatas\\jmdata-manages\\jmdata-manager-srteem\\target\\jmdata-manager-srteem-1.0.1.jar";
              
          
          if (args != null && args.length > 0) {
               String nimbus = args[0];
               conf.put(Config.NIMBUS_HOST, nimbus);
               conf.setNumWorkers(3);
               System.setProperty("storm.jar",jarstr);
               StormSubmitter.submitTopologyWithProgressBar(name, conf, builder.createTopology());
          } else {
               conf.setMaxTaskParallelism(3);
               LocalCluster cluster = new LocalCluster();
               cluster.submitTopology(name, conf, builder.createTopology());
               Thread.sleep(600);
              // cluster.shutdown();
          }
     }

}
