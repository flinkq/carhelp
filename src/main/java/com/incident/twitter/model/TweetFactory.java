package com.incident.twitter.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class TweetFactory
{
    public static Tweet build(JSONObject twitterJson)
    {
	Tweet tweet = new Tweet(parseCreatedAt(twitterJson), parseId(twitterJson), parseTweet(twitterJson), TwitterProfileFactory.build(twitterJson));
	try
	{
	    tweet.setHashtags(parseHashtags(twitterJson));
	} catch (JSONException e)
	{
	    LoggerFactory.getLogger(TweetFactory.class).debug("Failed to parse tweet hashtags for {}: {}", tweet.getId(), e.toString());
	}
	return tweet;
    }

    private static Instant parseCreatedAt(JSONObject twitterJson)
    {
	return Instant.ofEpochMilli(twitterJson.getLong("timestamp_ms"));
    }

    private static Long parseId(JSONObject twitterJson)
    {
	return twitterJson.getLong("id");
    }

    private static String parseTweet(JSONObject twitterJson)
    {
	String tweet;
	if (twitterJson.toString().contains("retweeted_status"))
	{
	    try
	    {
		//if original tweet
		tweet = twitterJson.getJSONObject("retweeted_status")
				.getJSONObject("extended_tweet")
				.getString("full_text");
	    } catch (JSONException e)
	    {
		tweet = twitterJson.getJSONObject("retweeted_status")
				.getString("text");
	    }
	}
	else
	{
	    try
	    {
		//if original tweet
		tweet = twitterJson.getJSONObject("extended_tweet").getString("full_text").trim();
	    } catch (JSONException e)
	    {
		tweet = twitterJson.getString("full_text").trim();
	    }
	}
	return tweet;
    }

    private static Set<String> parseHashtags(JSONObject twitterJson)
    {
	Set<String> hashtags = new HashSet<>();
	if (twitterJson.toString().contains("retweeted_status")){
	    try{
		twitterJson.getJSONObject("retweeted_status")
				.getJSONObject("extended_tweet")
				.getJSONObject("entities").getJSONArray("hashtags").forEach(hashtag -> {
		    JSONObject hashtagJson = (JSONObject) hashtag;
		    hashtags.add(hashtagJson.getString("text"));
		});
	    }catch (JSONException e){
		twitterJson.getJSONObject("retweeted_status")
				.getJSONObject("entities").getJSONArray("hashtags").forEach(hashtag -> {
		    JSONObject hashtagJson = (JSONObject) hashtag;
		    hashtags.add(hashtagJson.getString("text"));
		});
	    }
	}else {
	    try{
		twitterJson.getJSONObject("extended_tweet")
				.getJSONObject("entities").getJSONArray("hashtags").forEach(hashtag -> {
		    JSONObject hashtagJson = (JSONObject) hashtag;
		    hashtags.add(hashtagJson.getString("text"));
		});
	    }catch (JSONException e){
		twitterJson.getJSONObject("entities").getJSONArray("hashtags").forEach(hashtag -> {
		    JSONObject hashtagJson = (JSONObject) hashtag;
		    hashtags.add(hashtagJson.getString("text"));
		});
	    }
	}
	return hashtags;
    }

}
