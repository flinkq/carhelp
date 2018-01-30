package com.incident.twitter.core;

import com.incident.twitter.filter.RetweetFilter;
import com.incident.twitter.filter.TMCAccidentsFilter;
import com.incident.twitter.filter.TweetFilter;
import com.incident.twitter.mapper.TweetMapper;
import com.incident.twitter.model.Tweet;
import com.incident.twitter.service.LocationService;
import com.incident.twitter.service.impl.GoogleLocationService;
import com.incident.twitter.sink.SimpleRedisSink;
import com.incident.twitter.sink.SlackSink;
import com.incident.twitter.util.ElasticUtils;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.twitter.TwitterSource;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

public class Worker
{
    static Logger logger = Logger.getLogger(Worker.class);

    private static final String elasticHost = "142.44.243.86";
    private static final String elasticCluster = "test-cluster";
    private static final FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost("127.0.0.1").build();

    public static void main(String[] args) throws Exception
    {
	ParameterTool params = ParameterTool.fromArgs(args);
	StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);
	/////////////////////////////////////////
	//adding props TODO should be from params
	Properties props = new Properties();
	props.setProperty(TwitterSource.CONSUMER_KEY, "PNB7I9WfZHiCgSIl0RfZZ9Eqv");
	props.setProperty(TwitterSource.CONSUMER_SECRET, "dOyRZCdta5BGWwhGhjyfKQMV7fbT0Oi4Uifm8r82RnInpua45w");
	props.setProperty(TwitterSource.TOKEN, "1283394614-QkF3rjFqU3JwSoththtl1pDdYRBK77gMwoJFiTZ");
	props.setProperty(TwitterSource.TOKEN_SECRET, "d08V9Hwe7NnfdJB6tI8N6XjdXKS1rs5DItR5T8FDkb5qY");
	/////////////////////////////////////////
	//adding twitter source
	TwitterSource source = new TwitterSource(props);
	source.setCustomEndpointInitializer(new TMCLebanonFilter());
	/////////////////////////////////////////
	DataStream<String> streamSource = env.addSource(source).filter(new TweetFilter());
	streamSource.print();
	/////////////////////////////////////////
	//saving all data that is coming
	streamSource.addSink(new SimpleRedisSink(conf, "queues:tweets:tmc"));
	/////////////////////////////////////////
	//filters
	DataStream<Tweet> accidentsStream = streamSource
			.filter(new RetweetFilter())
			.map(new TweetMapper())
			.filter(new TMCAccidentsFilter())
			.map(Worker::getLocations);
	/////////////////////////////////////////
	//saving the data
	accidentsStream.addSink(ElasticUtils.getElasticSink("twitter", "accidents", elasticHost, elasticCluster));
	/////////////////////////////////////////
	//notifying with slack
	accidentsStream.addSink(new SlackSink());
	//addElasticRawSink(rawStream);
	//addRedisEnrichedSink(enrichedStream);
	//addElasticEnrichedSink(enrichedStream);
	/////////////////////////////////////////
	env.execute("Twitter Streaming Example");
    }

    private static Tweet getLocations(Tweet tweet)
    {
	logger.debug("Getting locations for tweet");
	LocationService locationService = GoogleLocationService.getInstance();
	tweet.getHashtags().stream().map(hashtag -> locationService.detectLocation(hashtag))
			.filter(Optional::isPresent).findAny().map(Optional::get).ifPresent(tweet::setAccidentLocaiton);
	tweet.getAccidentLocaiton().ifPresent(location -> logger.debug("Found accident location " + location.getName()));
	return tweet;
    }

    public static class TMCLebanonFilter implements TwitterSource.EndpointInitializer, Serializable
    {
	@Override public StreamingEndpoint createEndpoint()
	{
	    StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
	    endpoint.followings(Collections.singletonList(2236553426L));
	    return endpoint;
	}
    }
}


