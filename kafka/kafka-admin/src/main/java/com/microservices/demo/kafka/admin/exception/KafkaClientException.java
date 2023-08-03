package com.microservices.demo.kafka.admin.exception;

/**
 * This class has only one sole purpose to provide the custom exception message by extending the RuntimeException so that we will be easily identify which module
 * causing the exception.
 */
public class KafkaClientException extends RuntimeException{
    public KafkaClientException() {
    }
    public KafkaClientException(String message){
        super(message);
    }
    public KafkaClientException(String message, Throwable cause){
        super(message,cause);
    }
}
