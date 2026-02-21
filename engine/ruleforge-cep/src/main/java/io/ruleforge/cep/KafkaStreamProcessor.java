package io.ruleforge.cep;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;

import java.time.Duration;
import java.util.Properties;

/**
 * Builds Kafka Streams topologies for CEP pattern processing.
 * Each CEP definition translates into a Kafka Streams topology that:
 * <ol>
 *   <li>Consumes events from the input topic</li>
 *   <li>Windows events according to the time window configuration</li>
 *   <li>Produces results to the output topic when patterns are matched</li>
 * </ol>
 */
public final class KafkaStreamProcessor {

    private final CepDefinition cepDefinition;

    public KafkaStreamProcessor(CepDefinition cepDefinition) {
        this.cepDefinition = cepDefinition;
    }

    /**
     * Builds a Kafka Streams topology for this CEP definition.
     *
     * @param builder the streams builder to add the topology to
     * @return the output stream producing pattern match results
     */
    public KStream<String, String> buildTopology(StreamsBuilder builder) {
        TimeWindow window = cepDefinition.timeWindow();

        KStream<String, String> inputStream = builder.stream(
                cepDefinition.inputTopic(),
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // Apply windowing based on window type
        Duration windowDuration = window.duration();
        Duration graceDuration = window.graceTime();

        KStream<String, String> processedStream = inputStream
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeAndGrace(windowDuration, graceDuration))
                .reduce((oldVal, newVal) -> oldVal + ";" + newVal)
                .toStream()
                .map((windowedKey, value) -> new org.apache.kafka.streams.KeyValue<>(
                        windowedKey.key(), value));

        // Write to output topic
        if (cepDefinition.outputTopic() != null) {
            processedStream.to(cepDefinition.outputTopic(),
                    Produced.with(Serdes.String(), Serdes.String()));
        }

        return processedStream;
    }

    /**
     * Returns the recommended Kafka Streams configuration properties for this CEP definition.
     *
     * @param applicationId the Kafka Streams application ID
     * @param bootstrapServers the Kafka bootstrap servers
     * @return configuration properties
     */
    public Properties getStreamProperties(String applicationId, String bootstrapServers) {
        Properties props = new Properties();
        props.put("application.id", applicationId);
        props.put("bootstrap.servers", bootstrapServers);
        props.put("default.key.serde", Serdes.StringSerde.class.getName());
        props.put("default.value.serde", Serdes.StringSerde.class.getName());
        props.put("commit.interval.ms", "1000");
        return props;
    }

    public CepDefinition getCepDefinition() {
        return cepDefinition;
    }
}
