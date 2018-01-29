package com.incident.twitter.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.incident.twitter.model.Tweet;
import com.incident.twitter.model.TweetFactory;
import com.incident.twitter.service.LocationService;
import com.incident.twitter.service.impl.GoogleLocationService;
import com.incident.twitter.util.ElasticUtils;
import com.incident.twitter.util.ObjectMapperFactory;
import com.incident.twitter.util.SlackNotifier;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.endpoint.StreamingEndpoint;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.collector.selector.OutputSelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SplitStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.apache.flink.streaming.connectors.twitter.TwitterSource;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.*;

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

	Properties props = new Properties();
	props.setProperty(TwitterSource.CONSUMER_KEY, "PNB7I9WfZHiCgSIl0RfZZ9Eqv");
	props.setProperty(TwitterSource.CONSUMER_SECRET, "dOyRZCdta5BGWwhGhjyfKQMV7fbT0Oi4Uifm8r82RnInpua45w");
	props.setProperty(TwitterSource.TOKEN, "1283394614-QkF3rjFqU3JwSoththtl1pDdYRBK77gMwoJFiTZ");
	props.setProperty(TwitterSource.TOKEN_SECRET, "d08V9Hwe7NnfdJB6tI8N6XjdXKS1rs5DItR5T8FDkb5qY");

	TwitterSource source = new TwitterSource(props);
	source.setCustomEndpointInitializer(new TMCLebanonFilter());
	DataStream<String> streamSource = env.addSource(source);
	streamSource.print();
	//filters
	SplitStream<JSONObject> twitterSplitStream = streamSource
			.filter(twitterStr -> twitterStr != null && !twitterStr.trim().isEmpty() && isValidJson(twitterStr))
			//Evaluates a boolean function for each element and retains those for which the function returns true.
			.filter(new FilterFunction<String>()
			{
			    @Override public boolean filter(String twitterStr) throws Exception
			    {
				if (params.getBoolean("save.retweets", false))
				    return true;
				else
				    return !twitterStr.contains("retweeted_status");
			    }
			}).map(twitterStr -> new JSONObject(twitterStr)).filter(twitterJson -> twitterJson.optLong("timestamp_ms") != 0)
			//we have good tweets here
			.split(new OutputSelector<JSONObject>()
			{
			    @Override public Iterable<String> select(JSONObject value)
			    {
				List<String> splitStreams = new ArrayList<>();
				splitStreams.add("raw");
				splitStreams.add("enriched");
				return splitStreams;
			    }
			});
	//
	DataStream<Tweet> enrichedStream = twitterSplitStream.select("enriched").map(TweetFactory::build) //building tweet
			.filter(tweet -> tweet.getHashtags().contains("كفانا_بقى") || tweet.getText().contains("كفانا_بقى") || tweet.getText()
					.contains("تصادم") || tweet.getText().contains("حادث")) //this hashtag means accident
			.map(tweet -> getLocations(tweet))
			//we got the ones with location
			.map(tweet -> {
			    tweet.getAccidentLocaiton().ifPresent(location -> {
				SlackNotifier.notify("Detected at " + location.getName());
				try
				{
				    //TwilioNotifier.notify("Detected accident at " + location.getName() + ", " + location.getCountry());
				} catch (Exception e)
				{
				}
			    });
			    return tweet;
			});

	DataStream<JSONObject> rawStream = twitterSplitStream.select("raw");
	addRedisRawSink(rawStream);
	//        addElasticRawSink(rawStream);
	addRedisEnrichedSink(enrichedStream);
	//	addElasticEnrichedSink(enrichedStream);
	env.execute("Twitter Streaming Example");
    }

    private static Tweet getLocations(Tweet tweet)
    {
	logger.debug("Getting locations for tweet");
	LocationService locationService = GoogleLocationService.getInstance();
	tweet.getHashtags().stream().map(hashtag -> locationService.detectLocation(tweet.getTwitterProfile().getCountry(), hashtag))
			.filter(Optional::isPresent).findAny().map(Optional::get).ifPresent(tweet::setAccidentLocaiton);
	tweet.getAccidentLocaiton().ifPresent(location -> logger.debug("Found accident location " + location.getName()));
	return tweet;
    }

    private static void addRedisRawSink(DataStream<JSONObject> rawStream)
    {
	rawStream.addSink(new RedisSink<>(conf, new RedisMapper<JSONObject>()
	{
	    @Override public RedisCommandDescription getCommandDescription()
	    {
		return new RedisCommandDescription(RedisCommand.LPUSH);
	    }

	    @Override public String getKeyFromData(JSONObject tweet)
	    {
		return "queue:tweets:raw";
	    }

	    @Override public String getValueFromData(JSONObject tweet)
	    {
		return tweet.toString();
	    }
	}));
    }

    private static void addRedisEnrichedSink(DataStream<Tweet> enrichedStream)
    {
	enrichedStream.addSink(new RedisSink<>(conf, new RedisMapper<Tweet>()
	{
	    @Override public RedisCommandDescription getCommandDescription()
	    {
		return new RedisCommandDescription(RedisCommand.LPUSH);
	    }

	    @Override public String getKeyFromData(Tweet tweet)
	    {
		StringBuilder key = new StringBuilder("queue:tweets:enriched:" + tweet.getTwitterProfile().getHandle());
		tweet.getAccidentLocaiton().ifPresent(location -> key.append(":").append(location.getName()));
		return key.toString();
	    }

	    @Override public String getValueFromData(Tweet tweet)
	    {
		try
		{
		    return ObjectMapperFactory.getObjectMapper().writeValueAsString(tweet);
		} catch (JsonProcessingException e)
		{
		    return "Error serializing tweet " + tweet.getId();
		}
	    }
	}));
    }

    private static void addElasticEnrichedSink(DataStream<Tweet> enrichedStream) throws UnknownHostException
    {
	enrichedStream.map(tweet -> {
	    return (HashMap<String, Object>) ObjectMapperFactory.getObjectMapper().convertValue(tweet, HashMap.class);
	}).returns(new TypeHint<HashMap<String, Object>>()
	{
	    @Override public TypeInformation<HashMap<String, Object>> getTypeInfo()
	    {
		return super.getTypeInfo();
	    }
	}).addSink(ElasticUtils.getElasticSink("twitter", "enriched", elasticHost, elasticCluster));
    }

    private static void addElasticRawSink(DataStream<JSONObject> rawStream) throws UnknownHostException
    {
	rawStream.map(JSONObject::toMap).addSink(ElasticUtils.getElasticSink("twitter", "raw", elasticHost, elasticCluster));
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

    private static boolean isValidJson(String twitterStr)
    {
	try
	{
	    new JSONObject(twitterStr);
	    return true;
	} catch (JSONException e)
	{
	    return false;
	}
    }
}


