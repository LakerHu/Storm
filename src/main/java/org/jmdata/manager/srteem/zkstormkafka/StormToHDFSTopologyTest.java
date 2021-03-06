package org.jmdata.manager.srteem.zkstormkafka;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;

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
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

public class StormToHDFSTopologyTest {

     public static class EventSpout extends BaseRichSpout {

          private static final Log LOG = LogFactory.getLog(EventSpout.class);
          private static final long serialVersionUID = 886149197481637894L;
          private SpoutOutputCollector collector;
          private Random rand;
          private String[] records;
         
          public void open(Map conf, TopologyContext context,
                    SpoutOutputCollector collector) {
               this.collector = collector;    
               rand = new Random();
               records = new String[] {
                         "10001     ef2da82d4c8b49c44199655dc14f39f6     4.2.1     HUAWEI G610-U00     HUAWEI     2     70:72:3c:73:8b:22     2014-10-13 12:36:35",
                         "10001     ffb52739a29348a67952e47c12da54ef     4.3     GT-I9300     samsung     2     50:CC:F8:E4:22:E2     2014-10-13 12:36:02",
                         "10001     ef2da82d4c8b49c44199655dc14f39f6     4.2.1     HUAWEI G610-U00     HUAWEI     2     70:72:3c:73:8b:22     2014-10-13 12:36:35"
               };
          }


          public void nextTuple() {
               Utils.sleep(1000);
               DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
               Date d = new Date(System.currentTimeMillis());
               String minute = df.format(d);
               String record = records[rand.nextInt(records.length)];
               LOG.info("EMIT[spout -> hdfs] " + minute + " : " + record);
               collector.emit(new Values(minute, record));
          }

          public void declareOutputFields(OutputFieldsDeclarer declarer) {
               declarer.declare(new Fields("minute", "record"));         
          }


     }
    
     public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException, InterruptedException, AuthorizationException {
          // use "|" instead of "," for field delimiter
          RecordFormat format = new DelimitedRecordFormat()
                  .withFieldDelimiter(" : ");

          // sync the filesystem after every 1k tuples
          SyncPolicy syncPolicy = new CountSyncPolicy(1000);

          // rotate files 
          FileRotationPolicy rotationPolicy = new TimedRotationPolicy(1.0f, TimeUnit.MINUTES);

          FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                  .withPath("/jmhdfs/log/kafka/").withPrefix("app_").withExtension(".log");

          HdfsBolt hdfsBolt = new HdfsBolt()
                  .withFsUrl("hdfs://192.168.1.141:9000")
                  .withFileNameFormat(fileNameFormat)
                  .withRecordFormat(format)
                  .withRotationPolicy(rotationPolicy)
                  .withSyncPolicy(syncPolicy);
         
          TopologyBuilder builder = new TopologyBuilder();
          builder.setSpout("event-spout", new EventSpout(), 3);
          builder.setBolt("hdfs-bolt", hdfsBolt, 2).fieldsGrouping("event-spout", new Fields("minute"));
         
          Config conf = new Config();
         // args  = new String []{"bds1"};
          //String jarstr = "E:\\dev\\workspace\\jmms\\jmdata-libs\\jmdata-bigdatas\\jmdata-manages\\jmdata-manager-srteem\\target\\jmdata-manager-srteem-1.0.1.jar";
          String name = StormToHDFSTopologyTest.class.getSimpleName();
          if (args != null && args.length > 0) {
               conf.put(Config.NIMBUS_HOST, "bds1");
               conf.setNumWorkers(3);
               //System.setProperty("storm.jar",jarstr);
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
