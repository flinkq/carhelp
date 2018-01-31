package com.incident.twitter.model;

import org.json.JSONObject;

public class TwitterProfileFactory
{
    public static TwitterProfile build(JSONObject tweetJson)
    {
	TwitterProfile twitterProfile = new TwitterProfile(parseId(tweetJson), parseHandle(tweetJson));
	return twitterProfile;
    }

    private static Long parseId(JSONObject tweetJson)
    {
	return tweetJson.getJSONObject("user").getLong("id");
    }

    private static String parseHandle(JSONObject tweetJson)
    {
	return tweetJson.getJSONObject("user").getString("screen_name");
    }
}
