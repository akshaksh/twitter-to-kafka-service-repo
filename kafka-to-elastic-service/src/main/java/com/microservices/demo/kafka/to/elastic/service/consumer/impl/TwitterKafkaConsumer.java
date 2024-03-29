package com.microservices.demo.kafka.to.elastic.service.consumer.impl;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.KafkaConsumerConfigData;
import com.microservices.demo.elastic.index.client.service.ElasticIndexClient;
import com.microservices.demo.kafka.admin.client.KafkaAdminClient;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import com.microservices.demo.kafka.to.elastic.service.consumer.KafkaConsumer;
import com.microservices.demo.kafka.to.elastic.service.transformer.AvroToElasticModelTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TwitterKafkaConsumer implements KafkaConsumer<Long, TwitterAvroModel> {
  private static final Logger LOG = LoggerFactory.getLogger(TwitterKafkaConsumer.class);
  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
  private final KafkaAdminClient kafkaAdminClient;
  private final KafkaConfigData kafkaConfigData;
  private final AvroToElasticModelTransformer avroToElasticModelTransformer;
  private final ElasticIndexClient<TwitterIndexModel> elasticIndexClient;
  private final KafkaConsumerConfigData kafkaConsumerConfigData;

  public TwitterKafkaConsumer(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry,
      KafkaAdminClient kafkaAdminClient, KafkaConfigData kafkaConfigData,
      AvroToElasticModelTransformer avroToElasticModelTransformer,
      ElasticIndexClient<TwitterIndexModel> elasticIndexClient,
      KafkaConsumerConfigData kafkaConsumerConfigData) {
    this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    this.kafkaAdminClient = kafkaAdminClient;
    this.kafkaConfigData = kafkaConfigData;
    this.avroToElasticModelTransformer = avroToElasticModelTransformer;
    this.elasticIndexClient = elasticIndexClient;
    this.kafkaConsumerConfigData = kafkaConsumerConfigData;
  }

  @EventListener
  public void onAppStarted(ApplicationStartedEvent event){
    kafkaAdminClient.checkTopicsCreated();
    LOG.info("Topics with name: {} is ready for operations! ",kafkaConfigData.getTopicNamesToCreate().toArray());
    //This will give us the listener container from the id that we specified in the @KafkaListener annotation and start it explicitly.
    //it means below line simply start the kafka listener consumer manually once we got that topics are created.
    kafkaListenerEndpointRegistry.getListenerContainer(kafkaConsumerConfigData.getConsumerGroupId()).start();
  }

  @Override
  @KafkaListener(id = "${kafka-consumer-config.consumer-group-id}", topics = "${kafka-config.topic-name}")
  public void receive(@Payload List<TwitterAvroModel> messages,
      @Header(KafkaHeaders.RECEIVED_KEY) List<Integer> keys,
      @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
      @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
    LOG.info("{} number of message received with keys {}, partitions {} and offsets {}, " +
            "sending it to elastic: Thread id {}",
        messages.size(),
        keys.toString(),
        partitions.toString(),
        offsets.toString(),
        Thread.currentThread().getId());
    List<TwitterIndexModel> twitterIndexModel=avroToElasticModelTransformer.getElasticModels(messages);
    List<String> documentIds=elasticIndexClient.save(twitterIndexModel);
    LOG.info("Documents saved to elastic search with ids : {}",documentIds);
  }
}


