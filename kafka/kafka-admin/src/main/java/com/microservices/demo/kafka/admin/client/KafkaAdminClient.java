package com.microservices.demo.kafka.admin.client;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.RetryConfigData;
import com.microservices.demo.kafka.admin.exception.KafkaClientException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * This class is main class of this module work wise, and will perform below operations
 * 1. Check constantly if topic exist or not with retry mechanism ( keep oin mind no retryTemplate used here manual using loop we are checking)
 * 2. Create new topics with retry template
 * 3. Check schema registry server is up and running or not, and use loop for retrying based on HTTPStatus got from the calling schema registry URL through
 * WebClient bean in async mode.
 */
@Component
public class KafkaAdminClient {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaAdminClient.class);

    private final KafkaConfigData kafkaConfigData;

    private final RetryConfigData retryConfigData;

    private final AdminClient adminClient;

    private final RetryTemplate retryTemplate;

    private final WebClient webClient;

    /**
     * This is a constructor which will inject the required configuration variable mention above
     * @param config
     * @param retryConfigData
     * @param client
     * @param template
     * @param webClient
     */
    public KafkaAdminClient(KafkaConfigData config,
                            RetryConfigData retryConfigData,
                            AdminClient client,
                            RetryTemplate template,
                            WebClient webClient) {
        this.kafkaConfigData = config;
        this.retryConfigData = retryConfigData;
        this.adminClient = client;
        this.retryTemplate = template;
        this.webClient=webClient;
    }


    /**
     * This method will simple create the topics if does not exist with retry logic and for that it will use
     * the spring retry template.
     */
    public void createTopics() {
        CreateTopicsResult createTopicsResult;
        try {
            createTopicsResult = retryTemplate.execute(this::doCreateTopics);
        } catch (Throwable t) {
            throw new KafkaClientException("Reached max number of retry for creating kafka topic(s) !",t);
        }

    }

    /**
     * This method will check whether topics are created or not if not then it will wait for few moment
     * and then will check again and this process will continue in infinite loop and will break in two cases
     * either topics are created or number of retry is exhausted.
     * As of now not using in this class or outside this class.
     */
    public void checkTopicsCreated() {
        Collection<TopicListing> topics=getTopics();
        int retryCount=1;
        Integer maxRetry=retryConfigData.getMaxAttempts();
        Integer multiplier=retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs=retryConfigData.getSleepTimeMs();
        for(String topic: kafkaConfigData.getTopicNamesToCreate()){
            while(!isTopicCreated(topics,topic)){
                checkMaxRetry(retryCount++, maxRetry);
                sleep(sleepTimeMs);
                sleepTimeMs *=multiplier;
                topics=getTopics();
            }
        }
    }

    /**
     * This method will check whether schema registry server is up and running or not as we
     * will use avro data format in our program. and for that it will continue to check in infinite loop
     * which will end when schema registry server will come up or number of retries exhausted.
     */
    public void checkSchemaRegistry(){
        int retryCount=1;
        Integer maxRetry=retryConfigData.getMaxAttempts();
        Integer multiplier=retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs=retryConfigData.getSleepTimeMs();
        while(!getSchemaRegistryStatus().is2xxSuccessful()){
            checkMaxRetry(retryCount++, maxRetry);
            sleep(sleepTimeMs);
            sleepTimeMs *=multiplier;
        }
    }

    /**
     * This method will actually perform a REST API call to our schema registry server URL
     * using WebClient in sync mode and based on the response we will decide server is up or not.
     * @return HttpStatusCode
     */
    private HttpStatus getSchemaRegistryStatus() {
        try {
            return webClient
                    .method(HttpMethod.GET)
                    .uri(kafkaConfigData.getSchemaRegistryUrl())
                    .exchange()
                    .map(ClientResponse::statusCode)
                    .block();
        } catch (Exception e) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
    }


    /**
     * This method simply make the current thread sleep for passed sleep time in ms as parameter.
     * @param sleepTimeMs
     */
    public void sleep(Long sleepTimeMs){
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            throw new KafkaClientException("Error while sleeping for waiting new created topics !!");
        }
    }

    /**
     * This method takes two input retry and maxRetry and simple check the current retry number is less than total number of retries and if not throw exception.
     * @param retry
     * @param maxRetry
     */
    public void checkMaxRetry(int retry, Integer maxRetry) {
        if(retry > maxRetry)
            throw new KafkaClientException("Reached max number of retry for creating kafka topic(s) !");
    }

    /**
     * This method takes two input list of created topics and current topic name that needs to check whether it is created or not.
     * Based on the code we will return true or false
     * @param topics
     * @param topicName
     * @return
     */
    public boolean isTopicCreated(Collection<TopicListing> topics, String topicName) {
        return null == topics?false:topics.stream().anyMatch(topic -> topic.name().equals(topicName));
    }

    /**
     * This method basically created the topic with proper partition, replication factor and topic name and for this it will use
     * KafkaAdmin object. If topic is already created then it won't do anything.
     * @param retryContext
     * @return
     */
    public CreateTopicsResult doCreateTopics(RetryContext retryContext) {
        List<String> topicNames = kafkaConfigData.getTopicNamesToCreate();
        LOG.info("Creating {} topic(s), attempt {} ", topicNames.size(), retryContext.getRetryCount());
        List<NewTopic> kafkaTopics = topicNames.stream().map(topic -> new NewTopic(
                topic.trim(),
                kafkaConfigData.getNumOfPartitions(),
                kafkaConfigData.getReplicationFactor())).collect(Collectors.toList());
        CreateTopicsResult result=adminClient.createTopics(kafkaTopics);
        return result;
    }

    /**
     * This will simply return the Collection of TopicListing object where each TopicListing represent one topic.
     * @return
     */
    public Collection<TopicListing> getTopics(){
        Collection<TopicListing> topics;
        try {
            topics = retryTemplate.execute(this::doGetTopics);
        } catch (Throwable t) {
            throw new KafkaClientException("Reached max number of retry for creating kafka topic(s) !",t);
        }
        return topics;
    }

    /**
     * Using KafkaAdmin object will get the list of the topics and the using foreach will print name of the topics present in the list.
     * @param retryContext
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Collection<TopicListing> doGetTopics(RetryContext retryContext) throws ExecutionException,InterruptedException {
        LOG.info("Reading kafka topic {}, attempt {}",kafkaConfigData.getTopicNamesToCreate().toArray(),retryContext.getRetryCount());
        Collection<TopicListing> topics=adminClient.listTopics().listings().get();
        if(null != topics)
            topics.forEach(topic -> LOG.info("Topic with name {}",topic.name()));
        return topics;
    }


}
