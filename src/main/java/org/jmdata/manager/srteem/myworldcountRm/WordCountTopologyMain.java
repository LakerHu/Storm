package org.jmdata.manager.srteem.myworldcountRm;

	
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;
import org.jmdata.manager.srteem.myworldcount.WordNormalizer;

public class WordCountTopologyMain {
	public static void main(String[] args) throws InterruptedException {
		
		Map conf = new HashMap();
		
		conf.put("wordsFile", "/myhome/heyapeng-test.txt");
		
		//定义一个Topology
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("word-reader", new WordReader());
		builder.setBolt("word-normalizer", new WordNormalizer()).shuffleGrouping("word-reader");
		builder.setBolt("word-counter", new WordCounter(),2).fieldsGrouping("word-normalizer", new Fields("line"));
		
	    
	    
        conf.put(Config.TOPOLOGY_WORKERS, 2);
        
        conf.put(Config.TOPOLOGY_DEBUG, false);
        
          args = new String[]{"WordCountTopologyMain"};
          
          if (args != null && args.length > 0) {
        	  try {
        		  String jarstr ="C:\\myhome\\usr\\workspace\\jmms\\jmdata-libs\\jmdata-bigdatas\\jmdata-manages\\jmdata-manager-srteem\\target\\jmdata-manager-srteem-1.0.1.jar";
                  conf.put(Config.STORM_ZOOKEEPER_SERVERS, Arrays.asList(new String[] {"jmnb", "jmnd", "jmne"}));
                  conf.put(Config.NIMBUS_SEEDS, Arrays.asList(new String[] {"jmnb", "jmnc"}));
                  System.setProperty("storm.jar",jarstr);
                  StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
				//StormSubmitter.submitTopologyWithProgressBar(args[0], conf, builder.createTopology());
				} catch (AlreadyAliveException e) {
					e.printStackTrace();
				} catch (InvalidTopologyException e) {
					e.printStackTrace();
				} catch (AuthorizationException e) {
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