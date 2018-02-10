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
import com.incident.twitter.util.SocketServer;
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
	////////////////////////////////////////
        //init the socket server on port 9092
	SocketServer.init();
	////////////////////////////////////////
	ParameterTool params = ParameterTool.fromArgs(args);
	StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.getConfig().setGlobalJobParameters(params);
	/////////////////////////////////////////
	//adding props TODO should be from params
	Properties props = new Properties();
	props.setProperty(TwitterSource.CONSUMER_KEY, "3D0xIwXoDSkGnfL3bTAARKqBF");
	props.setProperty(TwitterSource.CONSUMER_SECRET, "jN1wvvNK6AbRXZgJWa98ZMsBDflsR0H1B3zaYdxctbR6OoyFSC");
	props.setProperty(TwitterSource.TOKEN, "1283394614-dq01NwcHWIxVYFgkZTtdrJapdyLJznpaLeJ5LOr");
	props.setProperty(TwitterSource.TOKEN_SECRET, "fgZR4X7FQQH56PsbtZfNaiPajYOUrA185pgU9GTNbWOEu");
	/////////////////////////////////////////
	//adding twitter source
	TwitterSource source = new TwitterSource(props);
	source.setCustomEndpointInitializer(new TMCLebanonFilter());
	/////////////////////////////////////////
	DataStream<String> streamSource = env.addSource(source).filter(new TweetFilter());
	//add retweet filter if needed
	if(params.getBoolean("filterRetweets", false))
	{
	    logger.info("Adding retweets filter");
	    streamSource = streamSource.filter(new RetweetFilter());
	}
	streamSource.print();
	/////////////////////////////////////////
	//saving all data that is coming
	streamSource.addSink(new SimpleRedisSink(conf, "queues:tweets:tmc"));
	/////////////////////////////////////////
	//filters
	DataStream<Tweet> accidentsStream = streamSource
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


