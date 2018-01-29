package com.incident.twitter.sink;

import com.incident.twitter.model.Tweet;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

public class RedisLpushSink extends RedisSink<Tweet>
{
    public RedisLpushSink(FlinkJedisConfigBase flinkJedisConfigBase)
    {
	super(flinkJedisConfigBase, new RedisSinkMapper());
    }

    private static class RedisSinkMapper implements RedisMapper<Tweet>
    {

	@Override
	public RedisCommandDescription getCommandDescription()
	{
	    return new RedisCommandDescription(RedisCommand.LPUSH);
	}

	@Override
	public String getKeyFromData(Tweet tweet)
	{
	    return "queue:tweets:raw";
	}

	@Override
	public String getValueFromData(Tweet tweet)
	{
	    return tweet.toString();
	}
    }
}
