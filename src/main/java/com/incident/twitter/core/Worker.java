package com.incident.twitter.core;

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
        props.setProperty(TwitterSource.CONSUMER_KEY, "hKFcfe1eZkBHqTrmUhO84mRtL");
        props.setProperty(TwitterSource.CONSUMER_SECRET, "i7yB0NejleSyMNCLV3Opow87ENO38dJkqeqNvkMdtMBA9afEf8");
        props.setProperty(TwitterSource.TOKEN, "610785462-bM2EYRGMVHNYrgSYOtsJnCgZui5yyBLwPNd6HWzS");
        props.setProperty(TwitterSource.TOKEN_SECRET, "CNKMIQgZ7ijErrKqY3MIOnpAu7rzBh0eyt7JWo79upnoH");
//        DataStream<String> streamSource = env.addSource(new TwitterSource(props));

//        TweetFilter filter = new TweetFilter();
//        TwitterSource source = new TwitterSource(props);
//        source.setCustomEndpointInitializer(filter);

        TMCLebanonFilter filter = new TMCLebanonFilter();
        TwitterSource source = new TwitterSource(props);
        source.setCustomEndpointInitializer(filter);

        DataStream<String> streamSource = env.addSource(source);

        streamSource.print();

        FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost("127.0.0.1").build();

        streamSource
                .filter(new FilterFunction<String>() {
                    @Override
                    public boolean filter(String value) throws Exception {
                        return value != null && !StringUtils.isEmpty(value) && !StringUtils.isWhitespace(value) && isJSONValid(value);
                    }
                })
//                .map(ss -> new org.json.JSONObject(ss).getString("body"))
                .addSink(new RedisSink<String>(conf, new RedisMapper<String>() {
            @Override
            public RedisCommandDescription getCommandDescription() {
                return new RedisCommandDescription(RedisCommand.LPUSH);
            }

            @Override
            public String getKeyFromData(String o) {
                return "sm:flink:tweets:tmc";
            }

            @Override
            public String getValueFromData(String o) {
                return o;
            }
        }));

        env.execute("Twitter Streaming Example");


    }

    public static class TweetFilter implements TwitterSource.EndpointInitializer, Serializable {
        @Override
        public StreamingEndpoint createEndpoint() {
            StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
            endpoint.trackTerms(Arrays.asList("NASA", "Discovery", "Interstellar"));
            return endpoint;
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


