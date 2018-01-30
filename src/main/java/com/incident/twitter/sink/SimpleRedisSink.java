package com.incident.twitter.sink;

import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

public class SimpleRedisSink extends RedisSink<String>
{
    public SimpleRedisSink(FlinkJedisConfigBase flinkJedisConfigBase, String queue)
    {
	super(flinkJedisConfigBase, new RedisSinkMapper(queue));
    }

    private static class RedisSinkMapper implements RedisMapper<String>
    {
        public RedisSinkMapper(String queue)
	{
	    this.queue = queue;
	}
	private String queue;

	@Override
	public RedisCommandDescription getCommandDescription()
	{
	    return new RedisCommandDescription(RedisCommand.LPUSH);
	}

	@Override
	public String getKeyFromData(String tweet)
	{
	    return queue;
	}

	@Override
	public String getValueFromData(String tweet)
	{
	    return tweet;
	}
    }
}