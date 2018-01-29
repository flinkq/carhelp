package com.incident.twitter.filter;

import org.apache.flink.api.common.functions.FilterFunction;

/**
 * Filters retweets
 */
public class RetweetFilter implements FilterFunction<String>
{
    @Override
    public boolean filter(String twitterStr) throws Exception
    {
	return !twitterStr.contains("retweeted_status");
	//return true;
    }
}
