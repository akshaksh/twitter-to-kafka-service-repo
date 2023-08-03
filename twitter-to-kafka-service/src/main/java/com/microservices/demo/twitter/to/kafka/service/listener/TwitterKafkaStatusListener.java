package com.microservices.demo.twitter.to.kafka.service.listener;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.kafka.producer.service.KafkaProducer;
import com.microservices.demo.twitter.to.kafka.service.transformer.TwitterStatusToAvroTransformer;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.StatusAdapter;

/**
 * This class has only one purpose to receive the Status of the tweet and print the tweet out of it.
 * And this class extends StatusAdapter instead of implementing the StatusListener because in case of implementing the interface we would have
 * override all the methods along with onStauts() which we needed in this case.
 */
@Component
public class TwitterKafkaStatusListener extends StatusAdapter {

    private static final Logger LOG= LoggerFactory.getLogger(TwitterKafkaStatusListener.class);
    private final KafkaConfigData kafkaConfigData;
    private final KafkaProducer<Long, TwitterAvroModel> kafkaProducer;
    private final TwitterStatusToAvroTransformer twitterStatusToAvroTransformer;

    /**
     * inject the KafkaConfigData, KafkaProducer and TwitterStatusToAvroTransformer beans in this constructor.
     * @param kafkaConfigData
     * @param kafkaProducer
     * @param twitterStatusToAvroTransformer
     */
    public TwitterKafkaStatusListener(KafkaConfigData kafkaConfigData, KafkaProducer<Long, TwitterAvroModel> kafkaProducer, TwitterStatusToAvroTransformer twitterStatusToAvroTransformer) {
        this.kafkaConfigData = kafkaConfigData;
        this.kafkaProducer = kafkaProducer;
        this.twitterStatusToAvroTransformer = twitterStatusToAvroTransformer;
    }

    /**
     * Whenever a tweet is fetched from twitter this listener class will listen them and we can get them in this status method.
     * And inside this method we will transform the Status object into TwitterAvroModel object and then produce this TwitterAvroModel as kafka message to our newly created kafka topic named "twitter-topic".
     * This method will be called by one of the runner class which will actually connect with twitter and start the stream.
     * @param status
     */
    @Override
    public void onStatus(Status status) {
        LOG.info("Received status text {} sending to kafka topic {}", status.getText(),kafkaConfigData.getTopicName());
        TwitterAvroModel twitterAvroModel=twitterStatusToAvroTransformer.getTwitterAvroModelFromStatus(status);
        kafkaProducer.send(kafkaConfigData.getTopicName(),twitterAvroModel.getUserId(),twitterAvroModel);

    }
}
