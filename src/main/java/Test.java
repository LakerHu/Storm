
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.avro.AvroFlumeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import com.google.common.base.Optional;

public class Test {

	public static void main(String[] args) {
		String topic = "flume_kafka_channel_topic";
		Properties props = new Properties();

		props.put("bootstrap.servers", "192.168.1.50:9000,192.168.1.51:9092,192.168.1.52:9092");
		props.put("group.id", "logmonitor" + System.currentTimeMillis());
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", 1000);
		props.put("session.timeout.ms", 30000);
		props.put("request.timeout.ms", 40000);
		props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		props.put("auto.offset.reset", "latest"); // 从最新的开始读起

		KafkaConsumer<String, byte[]> consumer = null;

		try {
			consumer = new KafkaConsumer<String, byte[]>(props);

			consumer.subscribe(Arrays.asList(topic));

			ConsumerRecords<String, byte[]> records = consumer.poll(1000);
			if (records == null || records.isEmpty()) {
				return;
			}
			// records.count();
			for (ConsumerRecord<String, byte[]> record : records) {
				byte[] data = record.value();

				ByteArrayInputStream in = new ByteArrayInputStream(data);
				BinaryDecoder decoder = DecoderFactory.get().directBinaryDecoder(in, null);

				Optional<SpecificDatumReader<AvroFlumeEvent>> reader = Optional.absent();
				if (!reader.isPresent()) {
					reader = Optional.of(new SpecificDatumReader<AvroFlumeEvent>(AvroFlumeEvent.class));
				}
				AvroFlumeEvent event = reader.get().read(null, decoder);
				event.getBody();

				Event e = EventBuilder.withBody(event.getBody().array(), toStringMap(event.getHeaders()));
				System.out.println(e.getHeaders());
				System.out.println(new String(e.getBody()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (consumer != null) {
				consumer.close();
			}
		}
	}

	private static Map<String, String> toStringMap(Map<CharSequence, CharSequence> charSeqMap) {
		Map<String, String> stringMap = new HashMap<String, String>();
		for (Map.Entry<CharSequence, CharSequence> entry : charSeqMap.entrySet()) {
			stringMap.put(entry.getKey().toString(), entry.getValue().toString());
		}
		return stringMap;
	}
}
