package com.incident.twitter.filter;

import org.apache.flink.api.common.functions.FilterFunction;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Filters Stream for tweets
 */
public class TweetFilter implements FilterFunction<String>
{
    @Override public boolean filter(String twitterJson) throws Exception
    {
	return twitterJson != null && !twitterJson.trim().isEmpty() && isValidJson(twitterJson)
			&& new JSONObject(twitterJson).has("text");
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
