package com.microservices.demo.kafka.producer.service.impl;


import com.microservices.demo.kafka.producer.service.KafkaProducer;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PreDestroy;

/**
 * This class is a serviec class whose work is simply send the messages to kafka topic and for that it will use Long as message key and
 * TwitterAvroModel as the message value and for this we will use kafkatemplate bean and send() method  of it in async mode.
 * Then using callback method we will be able to know whether message successfully delivered or not.
 */
@Service
public class TwitterKafkaProducer implements KafkaProducer<Long, TwitterAvroModel> {

    private static final Logger LOG= LoggerFactory.getLogger(TwitterKafkaProducer.class);

    private KafkaTemplate<Long,TwitterAvroModel> kafkaTemplate;

    /**
     * inject KafkaTemplate bean using constructor injection
     * @param kafkaTemplate
     */
    public TwitterKafkaProducer(KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * This method will use send() method of kafkatempalte bean which will take three input topicName, key and message.
     * And produce the message to provided topic name and this send() method will return the object of ListenableFuture who has addCallback() method
     * which will trigger based on the result we got from send() method and based on that we will call the failure or success callback method configured below.
     * @param topicName
     * @param key
     * @param message
     */
    @Override
    public void send(String topicName, Long key, TwitterAvroModel message) {
        LOG.info("Sending message='{}' to  topic = '{}'",message,topicName);
        //Register Callable methods for handling events when the response return.
        ListenableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture = kafkaTemplate.send(topicName, key, message);
        addCallback(topicName, message, kafkaResultFuture);

    }

    /**
     * This weill close the kafkatempalt bean before destroying this current class bean "TwitterKafkaProducer"
     */
    @PreDestroy
    public void close(){
        if(null != kafkaTemplate){
            LOG.info("Closing Kafka Producer !!");
            kafkaTemplate.destroy();
        }
    }

    /**
     * This is callback method configured for the ListenableFuture interface and override failure and success method which will trigger based on the result we got from send() method
     * @param topicName
     * @param message
     * @param kafkaResultFuture
     */
    private void addCallback(String topicName, TwitterAvroModel message, ListenableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture) {
        kafkaResultFuture.addCallback(new ListenableFutureCallback<SendResult<Long, TwitterAvroModel>>() {
            @Override
            public void onFailure(Throwable ex) {
                LOG.error("Error while sending message {} to topic {}", message.toString(), topicName,ex);
            }

            @Override
            public void onSuccess(SendResult<Long, TwitterAvroModel> result) {
                RecordMetadata metadata=result.getRecordMetadata();
                LOG.debug("Received new metadata Topic: {}; Partition : {}; Offset : {}; Timestamp: {}; at Time {}",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        metadata.timestamp(),
                        System.nanoTime());
            }
        });
    }
}
