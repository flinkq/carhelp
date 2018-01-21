package com.incident.twitter.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.incident.twitter.model.Tweet;
import com.incident.twitter.model.TweetFactory;
import com.incident.twitter.util.ObjectMapperFactory;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.container.RedisContainer;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.apache.flink.streaming.connectors.twitter.TwitterSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class Worker {

    public static void main(String[] args) throws Exception {
        //# access_token,access_token_secret,consumer_key,consumer_key_secret
        //6lZxXoPABGsetbEZQlKoN7RrU,
        // hEXv566sJbZf0DjtN9TAHEmEYqLApwwDMaAdswMkGyptPCOLJW,
        // 864837163124436992-8rkva6GwEVzHcJqL2MCxtg3PA5zzqQN,
        // 01vcZIuPQpCzsp5CSSS6vhkjgge2tds2n0NaYnJDQyyw9


        // set up the execution  environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties props = new Properties();
        props.setProperty(TwitterSource.CONSUMER_KEY, "PNB7I9WfZHiCgSIl0RfZZ9Eqv");
        props.setProperty(TwitterSource.CONSUMER_SECRET, "dOyRZCdta5BGWwhGhjyfKQMV7fbT0Oi4Uifm8r82RnInpua45w");
        props.setProperty(TwitterSource.TOKEN, "1283394614-QkF3rjFqU3JwSoththtl1pDdYRBK77gMwoJFiTZ");
        props.setProperty(TwitterSource.TOKEN_SECRET, "d08V9Hwe7NnfdJB6tI8N6XjdXKS1rs5DItR5T8FDkb5qY");
//        DataStream<String> streamSource = env.addSource(new TwitterSource(props));

        TwitterSource source = new TwitterSource(props);
        source.setCustomEndpointInitializer(new TMCLebanonFilter());

        DataStream<String> streamSource = env.addSource(source);

        streamSource.print();

        FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost("127.0.0.1").build();

        streamSource
                .filter(twitterStr -> twitterStr != null && !twitterStr.trim().isEmpty() && isJSONValid(twitterStr))
                .map(twitterStr -> new JSONObject(twitterStr))
                .filter(twitterJson -> twitterJson.optLong("timestamp_ms") != 0)
                .map(TweetFactory::build)
                .addSink(new RedisSink<Tweet>(conf, new RedisMapper<Tweet>() {
            @Override
            public RedisCommandDescription getCommandDescription() {
                return new RedisCommandDescription(RedisCommand.LPUSH);
            }

            @Override
            public String getKeyFromData(Tweet tweet) {
                return "sm:flink:tweets:" + tweet.getTwitterProfile().getHandle();
            }

            @Override
            public String getValueFromData(Tweet tweet) {
                try {
                    return ObjectMapperFactory.getObjectMapper().writeValueAsString(tweet);
                } catch (JsonProcessingException e) {
                    return "Error serializing tweet " + tweet.getId();
                }
            }
        }));
        env.execute("Twitter Streaming Example");
    }

    public static class TweetFilter implements TwitterSource.EndpointInitializer, Serializable {
        @Override
        public StreamingEndpoint createEndpoint() {
            StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
//            endpoint.trackTerms(Arrays.asList("NASA", "Discovery", "Interstellar"));
//            endpoint.trackTerms(Arrays.asList("Collision", "Car", "Crash"));
            endpoint.followings(Arrays.asList(25073877L));
            return endpoint; //ok?yea
        }
    }

    public static class TMCLebanonFilter implements TwitterSource.EndpointInitializer, Serializable {
        @Override
        public StreamingEndpoint createEndpoint() {
            StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
            endpoint.followings(Collections.singletonList(2236553426L));
            return endpoint;
        }
    }


    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}


