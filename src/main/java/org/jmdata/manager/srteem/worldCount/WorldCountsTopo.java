package org.jmdata.manager.srteem.worldCount;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.utils.Utils;

public class WorldCountsTopo {
    
    public static void main(String[] args) throws Exception {  
        TopologyBuilder builder = new TopologyBuilder();   
        builder.setSpout("spout", new RandomSpout());  
        builder.setBolt("bolt", new SenqueceBolt()).shuffleGrouping("spout");
       /* Config conf = new Config();  
        conf.setDebug(false); 
		conf.put("wordsFile", "C:\\myhome\\usr\\hadoop-2.7.2\\LICENSE.txt");*/
		
        Map conf = new HashMap();
        
        conf.put(Config.TOPOLOGY_WORKERS, 2);
        
        conf.put(Config.TOPOLOGY_DEBUG, false);
        
		String zkhosts = "192.168.23.31:2181,192.168.23.33:2181,192.168.23.34:2181";
		
        HashMap nimbusHosts = new HashMap();//{"jmnb","jmnc"};//new String []{"jmnb","jmnc"}; 
        nimbusHosts.put("jmnb", "jmnb");
        nimbusHosts.put("jmnc", "jmnc");
        Iterable nimbusseeds = (Iterable) nimbusHosts.entrySet();
        Iterator iter = nimbusseeds.iterator(); 
        conf.put(Config.DEV_ZOOKEEPER_PATH, zkhosts);
        
        conf.put(Config.NIMBUS_SEEDS, "jmnb");//"['jmnb' , 'jmnc']");
        
		args = new String[] { "WorldCountsTopo" };
		
	    //conf.put(Config.STORM_ZOOKEEPER_SERVERS,zkConnect);
	   // conf.put(Config.STORM_LOCAL_HOSTNAME,"192.168.23.31");

	   //conf.put(Config.STORM_DO_AS_USER,"192.168.23.31");
	  // conf.put(Config.NIMBUS_THRIFT_PORT, "1234");
	   
        
        if (args != null && args.length > 0) {  
            //conf.setNumWorkers(3);  
            StormSubmitter.submitTopology(args[0], conf, builder.createTopology());  
        } else {  
  
            LocalCluster cluster = new LocalCluster();  
            cluster.submitTopology("WorldCountsTopo", conf, builder.createTopology());  
            Utils.sleep(100000);  
            cluster.killTopology("WorldCountsTopo");  
            cluster.shutdown();  
        }

    }  
}