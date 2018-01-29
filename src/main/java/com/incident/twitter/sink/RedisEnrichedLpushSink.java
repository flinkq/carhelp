package com.incident.twitter.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.incident.twitter.model.Tweet;
import com.incident.twitter.util.ObjectMapperFactory;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

public class RedisEnrichedLpushSink extends RedisSink<Tweet>
{
    public RedisEnrichedLpushSink(FlinkJedisConfigBase flinkJedisConfigBase)
    {
	super(flinkJedisConfigBase, new RedisSinkMapper());
    }

    private static class RedisSinkMapper implements RedisMapper<Tweet>
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
    }
}
