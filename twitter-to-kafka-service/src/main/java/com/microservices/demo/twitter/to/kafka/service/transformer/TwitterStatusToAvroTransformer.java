package com.microservices.demo.twitter.to.kafka.service.transformer;

import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import org.springframework.stereotype.Component;
import twitter4j.Status;

/**
 * This class is used to convert the Status class object into TwitterAvroModel object
 */
@Component
public class TwitterStatusToAvroTransformer {
    public TwitterAvroModel getTwitterAvroModelFromStatus(Status status){
        return TwitterAvroModel
                .newBuilder()
                .setId(status.getId())
                .setText(status.getText())
                .setUserId(status.getUser().getId())
                .setCreatedAt(status.getCreatedAt().getTime())
                .build();
    }
}
