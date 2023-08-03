package com.microservices.demo.twitter.to.kafka.service.runner;

import twitter4j.TwitterException;

/**
 * This interface is created so that we would implement it multiple ways and one of them will be active at a time
 * and we will use this interface in the main class so that we don't autowired each implementation separately
 */
public interface StreamRunner {
    void start() throws TwitterException;
}
