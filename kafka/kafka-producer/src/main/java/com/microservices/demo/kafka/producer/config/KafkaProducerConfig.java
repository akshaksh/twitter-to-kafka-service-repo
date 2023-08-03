package com.microservices.demo.kafka.producer.config;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.KafkaProducerConfigData;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *This class will use the configuration loaded in the class KafkaConfigData and kafkaProducerConfigData for the kafka and kafka producer from application.yml and provide
 * the KafkaTemplate bean which we will use to produce the messages in kafka topic.
 */
@Configuration
public class KafkaProducerConfig <K extends Serializable,V extends SpecificRecordBase>{
    private final KafkaConfigData kafkaConfigData;
    private final KafkaProducerConfigData kafkaProducerConfigData;

    /**
     * Constructor to initialize KafkaConfigData and KafkaProducerConfigData.
     * @param kafkaConfigData
     * @param kafkaProducerConfigData
     */
    public KafkaProducerConfig(KafkaConfigData kafkaConfigData, KafkaProducerConfigData kafkaProducerConfigData) {
        this.kafkaConfigData = kafkaConfigData;
        this.kafkaProducerConfigData = kafkaProducerConfigData;
    }

    /**
     * Setting the all properties of kafka producer in a Map whcih we will use during the creation of ProducerFactroy object which ultimately will create
     * KafkaTemplate bean for us.
     * @return
     */
    @Bean
    public Map<String,Object> producerConfig(){
        Map<String,Object> props=new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaConfigData.getBootstrapServers());
        props.put(kafkaConfigData.getSchemaRegistryUrlKey(),kafkaConfigData.getSchemaRegistryUrl());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,kafkaProducerConfigData.getKeySerializerClass());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,kafkaProducerConfigData.getValueSerializerClass());
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,kafkaProducerConfigData.getBatchSize() * kafkaProducerConfigData.getBatchSizeBoostFactor());
        props.put(ProducerConfig.LINGER_MS_CONFIG,kafkaProducerConfigData.getLingerMs());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,kafkaProducerConfigData.getCompressionType());
        props.put(ProducerConfig.ACKS_CONFIG,kafkaProducerConfigData.getAcks());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,kafkaProducerConfigData.getRequestTimeoutMs());
        props.put(ProducerConfig.RETRIES_CONFIG,kafkaProducerConfigData.getRetryCount());
        return props;
    }

    /**
     * This method simply return the ProducerFactory object by loading the properties of kafka producers.
     * @return
     */
    @Bean
    public ProducerFactory<K,V> producerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    /**
     * This method will return the KafkaTemplate bean which we will use to send the message on topic
     * @return
     */
    @Bean
    public KafkaTemplate<K,V> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }

}
