package com.microservices.demo.twitter.to.kafka.service.runner.impl.twitter4j;


import com.microservices.demo.config.TwitterToKafkaServiceConfigData;
import com.microservices.demo.twitter.to.kafka.service.listener.TwitterKafkaStatusListener;
import com.microservices.demo.twitter.to.kafka.service.runner.StreamRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import twitter4j.FilterQuery;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import javax.annotation.PreDestroy;
import java.util.Arrays;

/**
 * This class is used to stream the twitter with twitter API version 1 which is not in the use currently
 * so this class is just for study purpose how were we streaming tweets earlier before twitter API V2
 */
@Component
@ConditionalOnExpression("not ${twitter-to-kafka-service.enable-mock-tweets} && not ${twitter-to-kafka-service.enable-v2-tweets}")
public class TwitterKafkaStreamRunner implements StreamRunner {

    private static final Logger LOG= LoggerFactory.getLogger(TwitterKafkaStreamRunner.class);
    private TwitterStream twitterStream;
    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    /**
     * insert the TwitterToKafkaServiceConfigData and TwitterKafkaStatusListener class bean
     * @param twitterToKafkaServiceConfigData
     * @param twitterKafkaStatusListener
     */
    public TwitterKafkaStreamRunner(TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData, TwitterKafkaStatusListener twitterKafkaStatusListener) {
        this.twitterToKafkaServiceConfigData = twitterToKafkaServiceConfigData;
        this.twitterKafkaStatusListener = twitterKafkaStatusListener;
    }

    /**
     * Add listener to our TwitterStream class for streaming of tweets
     * @throws TwitterException
     */
    @Override
    public void start() throws TwitterException {
        twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(twitterKafkaStatusListener);
        addFilter();
    }

    /**
     * Add filter of our keywords to filter out the tweet. As default it would ready so many tweets so narrow down
     * the tweet stream list we have added these filter of Java,Kafka,Microservice and Elasticsearch.
     */
    private void addFilter() {
        String []keywords=twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);
        FilterQuery filterQuery=new FilterQuery(keywords);
        twitterStream.filter(filterQuery);
        LOG.info("Started filtering twitter stream for keywords {}", Arrays.toString(keywords));
    }

    /**
     * Before destroying the bean close the twitter stream
     */
    @PreDestroy
    public void shutdown(){
        if(null != twitterStream){
            LOG.info("Closing the twitter stream");
            twitterStream.shutdown();
        }
    }
}
