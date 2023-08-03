package com.microservices.demo.twitter.to.kafka.service.init.impl;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.twitter.to.kafka.service.init.StreamInitializer;

import com.microservices.demo.kafka.admin.client.KafkaAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class has only one sole purpose to create the topics before starting the application if does not exist and for that it implements the interface StreamInitializer.
 */
@Component
public class KafkaStreamInitializer implements StreamInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamInitializer.class);

    private final KafkaConfigData kafkaConfigData;

    private final KafkaAdminClient kafkaAdminClient;

    /**
     * inject the KafkaConfigData and KafkaAdminClient bean
     * @param configData
     * @param adminClient
     */
    public KafkaStreamInitializer(KafkaConfigData configData, KafkaAdminClient adminClient) {
        this.kafkaConfigData = configData;
        this.kafkaAdminClient = adminClient;
    }

    /**
     * initialize the topic creation if does not exist
     */
    @Override
    public void init() {
        kafkaAdminClient.createTopics();
        LOG.info("Topics with name {} is ready for operations!", kafkaConfigData.getTopicNamesToCreate());
    }
}
