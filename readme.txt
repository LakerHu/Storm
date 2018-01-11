 
 cp kafka_2.11-0.10.0.1.jar scala-library-2.11.8.jar metrics-core-2.2.0.jar snappy-java-1.1.2.6.jar zkclient-0.8.jar log4j-1.2.17.jar slf4j-api-1.7.21.jar jopt-simple-4.9.jar /myhome/usr/storm/lib/
 
 
 
 
 
 kafka-server-start.sh /myhome/usr/kafka/config/server.properties &
 create /jmdata ""
 
 kafka-topics.sh --create --zookeeper jmnb:2181,jmnd:2181,jmne:2181/kafka --replication-factor 1 --partitions 5 --topic myjmdata
 
 kafka-topics.sh --describe --zookeeper jmnb:2181,jmnd:2181,jmne:2181/kafka --topic myjmdata
 
 kafka-console-producer.sh --broker-list jmnb:9092,jmnd:9092,jmne:9092 --topic myjmdata
 
 kafka-console-consumer.sh  --zookeeper jmnb:2181,jmnd:2181,jmne:2181/kafka --from-beginning --topic myjmdata
 
 
 kafka-preferred-replica-election.sh --zookeeper jmnb:2181,jmnd:2181,jmne:2181/kafka 
 
 
storm jar jmdata-manager-srteem-1.0.1.jar org.jmdata.manager.srteem.zkstormkafka.DistributeWordTopology distributeWordTopologys
 
 
 
 
 
 