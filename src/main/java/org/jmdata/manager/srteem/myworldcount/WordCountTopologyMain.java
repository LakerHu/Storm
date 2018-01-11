package org.jmdata.manager.srteem.myworldcount;

	
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;

public class WordCountTopologyMain {
	public static void main(String[] args) throws InterruptedException {
		//定义一个Topology
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("word-reader", new WordReader());
		builder.setBolt("word-normalizer", new WordNormalizer()).shuffleGrouping("word-reader");
		builder.setBolt("word-counter", new WordCounter(),2).fieldsGrouping("word-normalizer", new Fields("word"));
		//配置
		Config conf = new Config();
		
		conf.put("wordsFile", "e:\\output.txt");
		conf.setDebug(true);
		conf.put(Config.NIMBUS_HOST,"bds1");
	
		
//	    Map conf = new HashMap();
//        conf.put(Config.TOPOLOGY_WORKERS, 2);
//        
//        conf.put(Config.TOPOLOGY_DEBUG, false);
//        
//        conf.put("wordsFile", "C:\\myhome\\usr\\hadoop-2.7.2\\LICENSE.txt");
        
		String zkhosts = "192.168.23.31:2181,192.168.23.33:2181,192.168.23.34:2181";
        String nimbusHost = "jmnb";
        System.setProperty("storm.jar","jmdata-manager-srteem-1.0.1.jar");
        
		//args = new String[] { zkConnect };
		
	    //conf.put(Config.STORM_ZOOKEEPER_SERVERS,zkConnect);
	   // conf.put(Config.STORM_LOCAL_HOSTNAME,"192.168.23.31");
	    //conf.put(Config.NIMBUS_SEEDS,"jmnb");
	    
		if (args != null && args.length > 0) {  
		     
			    try {
					//提交Topology
					conf.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 1);
					//conf.setNumWorkers(1);
					
					StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
				} catch (AlreadyAliveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidTopologyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (AuthorizationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  
		    } else {  
		    	//创建一个本地模式cluster
		      LocalCluster cluster = new LocalCluster();
		      cluster.submitTopology("WordCountTopologyMains", conf, builder.createTopology());
		      Utils.sleep(100);
		      cluster.killTopology("WordCountTopologyMains");
		      cluster.shutdown();
		    }  
		
	}
}