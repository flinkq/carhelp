package com.incident.twitter.mapper;

import com.incident.twitter.model.Tweet;
import com.incident.twitter.model.TweetFactory;
import org.apache.flink.api.common.functions.MapFunction;
import org.json.JSONObject;

public class TweetMapper implements MapFunction<String, Tweet>
{
    @Override
    public Tweet map(String o) throws Exception
    {
	return TweetFactory.build(new JSONObject(o));
    }
}
