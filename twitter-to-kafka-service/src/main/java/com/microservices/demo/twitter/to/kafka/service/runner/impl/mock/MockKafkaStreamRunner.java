package com.microservices.demo.twitter.to.kafka.service.runner.impl.mock;

import com.microservices.demo.config.TwitterToKafkaServiceConfigData;
import com.microservices.demo.twitter.to.kafka.service.listener.TwitterKafkaStatusListener;
import com.microservices.demo.twitter.to.kafka.service.runner.StreamRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is used to stream the tweet from mock source. Basically this class generate the tweet using the words of array
 * and add filter keywords in between generated tweets. And tweet will be in specific format with fields created_at, user, id and text.
 * And bean of this class object depends upon the condition which say allow mock enablement instead of actual twitter stream.
 */
@Component
@ConditionalOnExpression("${twitter-to-kafka-service.enable-mock-tweets} && not ${twitter-to-kafka-service.enable-v2-tweets}")
public class MockKafkaStreamRunner implements StreamRunner {

    private static final Logger LOG= LoggerFactory.getLogger(MockKafkaStreamRunner.class);
    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    private static final Random RANDOM=new Random();

    private static final String WORDS[]= new String[]{
            "Lorem","ipsum","dolor","sit","amet","consectetuer","adipiscing","elit","congue","massa","Fusce","magna","sed"
    };
    private static final String tweetAsRawJson = "{" +
            "\"created_at\":\"{0}\"," +
            "\"id\":\"{1}\"," +
            "\"text\":\"{2}\"," +
            "\"user\":{\"id\":\"{3}\"}" +
            "}";
    private static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    /**
     * inject the TwitterToKafkaServiceConfigData and TwitterKafkaStatusListener bean in this constructor.
     * @param twitterToKafkaServiceConfigData
     * @param twitterKafkaStatusListener
     */
    public MockKafkaStreamRunner(TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData, TwitterKafkaStatusListener twitterKafkaStatusListener) {
        this.twitterToKafkaServiceConfigData = twitterToKafkaServiceConfigData;
        this.twitterKafkaStatusListener = twitterKafkaStatusListener;
    }

    /**
     * We will start simulate the twitter stream with mock tweets in this method.
     * @throws TwitterException
     */
    @Override
    public void start() throws TwitterException {
        String keywords[]=twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);
        int minTweetLength= twitterToKafkaServiceConfigData.getMockMinTweetLength();
        int maxTweetLength= twitterToKafkaServiceConfigData.getMockMaxTweetLength();
        long sleepTimeMs= twitterToKafkaServiceConfigData.getMockSleepMs();
        LOG.info("Starting mock filtering twitter streams for keywords {}", Arrays.toString(keywords));
        simulateTwitterStream(keywords, minTweetLength, maxTweetLength, sleepTimeMs);
    }

    /**
     * This method actually generate the mock tweet create the Status class object from this generated
     * tweet and call the onStatus() method of our class TwitterKafkaStatusListener which will simply
     * convert the Status object into TwitterAvroModel object and publish them on the kafka topic.
     * @param keywords
     * @param minTweetLength
     * @param maxTweetLength
     * @param sleepTimeMs
     */
    private void simulateTwitterStream(String[] keywords, int minTweetLength, int maxTweetLength, long sleepTimeMs){
        Executors.newSingleThreadExecutor().submit(() -> {
            //Streaming data infinitely
            try {
                while (true) {
                    String formattedTweetAsRawJson = getFormattedTweet(keywords, minTweetLength, maxTweetLength);
                    Status status = TwitterObjectFactory.createStatus(formattedTweetAsRawJson);
                    twitterKafkaStatusListener.onStatus(status);
                    sleep(sleepTimeMs);
                }
            }catch(TwitterException e){
                LOG.error("Error creating twitter status! ",e);
            }
        });
    }

    /**
     * Sleep the current thread for provided millisecond time
     * @param sleepTimeMs
     */
    private void sleep(long sleepTimeMs){
        try {
            Thread.sleep(sleepTimeMs);
        }catch(Exception exception){
            throw new RuntimeException("Error while sleeping for waiting new status to create !!");
        }
    }

    /**
     * Provide the parameters whose value we will use while replacing the placeholder shown in the
     * variable tweetAsRawJson and after replacing the placeholder with these parameters we have our actual twwet
     * with proper format (user,createdAt,text,id).
     * @param keywords
     * @param minTweetLength
     * @param maxTweetLength
     * @return
     */
    private String getFormattedTweet(String[] keywords, int minTweetLength, int maxTweetLength) {
        String[] params= new String[]{
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern(TWITTER_STATUS_DATE_FORMAT, Locale.ENGLISH)),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)),
                getRandomTweetContent(keywords,minTweetLength,maxTweetLength),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))

        };
        return formatTweetAsJsonWithParams(params);
    }

    /**
     * In this method we will have parameters array passed as input and we need to replace the placeholder
     * shown in the above variable tweetAsRawJson like {0}, {1} etc..
     * @param params
     * @return
     */
    private String formatTweetAsJsonWithParams(String[] params) {
        String tweet=tweetAsRawJson;
        for(int i = 0; i< params.length; i++){
            tweet=tweet.replace("{"+i+"}", params[i]);
        }
        return tweet;
    }

    /**
     * Generate random tweet and in between tweet inserting our Elasticsearch criteria keywords Java, Microservice,
     * Kafka and Elasticsearch and tweet text limit will also define using min and max length.
     * @param keywords
     * @param minTweetLength
     * @param maxTweetLength
     * @return
     */
    private String getRandomTweetContent(String[] keywords, int minTweetLength, int maxTweetLength) {
        StringBuilder tweet =new StringBuilder();
        int tweetLength=RANDOM.nextInt(maxTweetLength-minTweetLength+1) + minTweetLength;
        return constructRandomTweet(keywords, tweet, tweetLength);
    }

    /**
     * This method will help the getRandomTweetContent() method to generate the random tweet for our mock tweet streaming.
     * @param keywords
     * @param tweet
     * @param tweetLength
     * @return
     */
    private String constructRandomTweet(String[] keywords, StringBuilder tweet, int tweetLength) {
        for(int i = 0; i< tweetLength; i++){
            tweet.append(WORDS[RANDOM.nextInt(WORDS.length)]).append(" ");
            if(i == tweetLength /2)
                tweet.append((keywords[RANDOM.nextInt(keywords.length)])).append(" ");
        }
        return tweet.toString().trim();
    }
}
