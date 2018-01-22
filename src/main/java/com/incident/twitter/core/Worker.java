package com.incident.twitter.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.incident.twitter.model.Location;
import com.incident.twitter.model.Tweet;
import com.incident.twitter.model.TweetFactory;
import com.incident.twitter.service.LocationService;
import com.incident.twitter.service.impl.GoogleLocationService;
import com.incident.twitter.util.ObjectMapperFactory;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.collector.selector.OutputSelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SplitStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSink;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.apache.flink.streaming.connectors.twitter.TwitterSource;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Worker {
    private static final String elasticHost = "localhost";
    private static final String elasticCluster = "elasticsearch_issakhoury";
    private static final FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost("127.0.0.1").build();

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties props = new Properties();
        props.setProperty(TwitterSource.CONSUMER_KEY, "PNB7I9WfZHiCgSIl0RfZZ9Eqv");
        props.setProperty(TwitterSource.CONSUMER_SECRET, "dOyRZCdta5BGWwhGhjyfKQMV7fbT0Oi4Uifm8r82RnInpua45w");
        props.setProperty(TwitterSource.TOKEN, "1283394614-QkF3rjFqU3JwSoththtl1pDdYRBK77gMwoJFiTZ");
        props.setProperty(TwitterSource.TOKEN_SECRET, "d08V9Hwe7NnfdJB6tI8N6XjdXKS1rs5DItR5T8FDkb5qY");

        TwitterSource source = new TwitterSource(props);
//        source.setCustomEndpointInitializer(new TMCLebanonFilter());
        DataStream<String> streamSource = env.addSource(source);
        SplitStream<JSONObject> twitterSplitStream = streamSource
                .filter(twitterStr -> twitterStr != null && !twitterStr.trim().isEmpty())
                .map(twitterStr -> new JSONObject(twitterStr))
                .filter(twitterJson -> twitterJson.optLong("timestamp_ms") != 0)
                .split(new OutputSelector<JSONObject>() {
                    @Override
                    public Iterable<String> select(JSONObject value) {
                        List<String> splitStreams = new ArrayList<>();
                        splitStreams.add("raw");
                        splitStreams.add("enriched");
                        return splitStreams;
                    }
                });
        DataStream<Tweet> enrichedStream = twitterSplitStream.select("enriched")
                .map(TweetFactory::build)
                .map(tweet -> {
                    LocationService locationService = GoogleLocationService.getInstance();
                    Optional<Location> accidentLocation = locationService.detectLocation(tweet.getHashtags());
                    accidentLocation.ifPresent(tweet::setAccidentLocaiton);
                    return tweet;
                });
                //Enable when location implementation tested and completed
//                .filter(tweet -> tweet.getAccidentLocaiton().isPresent());

        DataStream<JSONObject> rawStream = twitterSplitStream.select("raw");
//        addRedisEnrichedSink(enrichedStream);
        addRedisRawSink(rawStream);
        addElasticRawSink(rawStream);
//        addElasticEnrichedSink(enrichedStream);

        env.execute("Twitter Streaming Example");
    }

    public static class TMCLebanonFilter implements TwitterSource.EndpointInitializer, Serializable {
        @Override
        public StreamingEndpoint createEndpoint() {
            StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
            endpoint.followings(Collections.singletonList(2236553426L));
            return endpoint;
        }
    }
    private static void addRedisRawSink(DataStream<JSONObject> rawStream){
        rawStream .addSink(new RedisSink<>(conf, new RedisMapper<JSONObject>() {
            @Override
            public RedisCommandDescription getCommandDescription() {
                return new RedisCommandDescription(RedisCommand.LPUSH);
            }

            @Override
            public String getKeyFromData(JSONObject tweet) {
                return "sm:flink:tweets:raw";
            }

            @Override
            public String getValueFromData(JSONObject tweet) {
                return tweet.toString();
            }
        }));
    }
    private static void addRedisEnrichedSink(DataStream<Tweet> enrichedStream){
        enrichedStream.addSink(new RedisSink<>(conf, new RedisMapper<Tweet>() {
            @Override
            public RedisCommandDescription getCommandDescription() {
                return new RedisCommandDescription(RedisCommand.LPUSH);
            }

            @Override
            public String getKeyFromData(Tweet tweet) {
                return "sm:flink:tweets:enriched:" + tweet.getTwitterProfile().getHandle();
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
    }
    private static void addElasticRawSink(DataStream<JSONObject> rawStream) throws UnknownHostException {
        Map<String, String> config = new HashMap<>();
        config.put("cluster.name", elasticCluster);
        // This instructs the sink to emit after every element, otherwise they would be buffered
        config.put("bulk.flush.max.actions", "1");

        List<TransportAddress> transportAddresses = new ArrayList<>();
        transportAddresses.add(new InetSocketTransportAddress(InetAddress.getByName(elasticHost), 9300));

        rawStream.addSink(new ElasticsearchSink<>(config, transportAddresses, new ElasticsearchSinkFunction<JSONObject>() {
            public IndexRequest createIndexRequest(JSONObject element) {
                return Requests.indexRequest()
                        .index("twitter")
                        .type("raw")
                        .source(element.toString());
            }

            @Override
            public void process(JSONObject element, RuntimeContext ctx, RequestIndexer indexer) {
                indexer.add(createIndexRequest(element));
            }
        }));
    }
    private static void addElasticEnrichedSink(DataStream<Tweet> enrichedStream) throws UnknownHostException {
        Map<String, String> config = new HashMap<>();
        config.put("cluster.name", elasticCluster);
        // This instructs the sink to emit after every element, otherwise they would be buffered
        config.put("bulk.flush.max.actions", "1");

        List<TransportAddress> transportAddresses = new ArrayList<>();
        transportAddresses.add(new InetSocketTransportAddress(InetAddress.getByName(elasticHost), 9300));

        enrichedStream.addSink(new ElasticsearchSink<>(config, transportAddresses, new ElasticsearchSinkFunction<Tweet>() {
            public IndexRequest createIndexRequest(Tweet element) throws JsonProcessingException {
                return Requests.indexRequest()
                        .index("twitter")
                        .type("enriched")
                        .source(ObjectMapperFactory.getObjectMapper().writeValueAsString(element));
            }

            @Override
            public void process(Tweet element, RuntimeContext ctx, RequestIndexer indexer) {
                try {
                    indexer.add(createIndexRequest(element));
                } catch (JsonProcessingException e) {
                    LoggerFactory.getLogger(this.getClass())
                            .error("Failed to index tweet {} in elastic", element.getId(), e);
                }
            }
        }));
    }
}


