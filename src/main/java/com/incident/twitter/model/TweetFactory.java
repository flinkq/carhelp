package com.incident.twitter.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.util.ajax.JSON;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class TweetFactory {
    public static Tweet build(JSONObject twitterJson){
        Tweet tweet = new Tweet(parseCreatedAt(twitterJson),
                parseId(twitterJson),
                parseTweet(twitterJson),
                TwitterProfileFactory.build(twitterJson));
        try{
            tweet.setHashtags(parseHashtags(twitterJson));
        }catch (JSONException e){
            LoggerFactory.getLogger(TweetFactory.class).debug("Failed to parse tweet hashtags for {}: {}", tweet.getId(), e.toString());
        }
        return tweet;
    }
    private static Instant parseCreatedAt(JSONObject twitterJson){
        return Instant.ofEpochMilli(twitterJson.getLong("timestamp_ms"));
    }
    private static Long parseId(JSONObject twitterJson){
        return twitterJson.getLong("id");
    }
    private static String parseTweet(JSONObject twitterJson){
        return twitterJson.getString("text").trim();
    }
    private static Set<String> parseHashtags(JSONObject twitterJson){
        Set<String> hashtags = new HashSet<>();
        twitterJson.getJSONObject("entities")
                .getJSONArray("hashtags")
                .forEach(hashtag -> {
                    JSONObject hashtagJson = (JSONObject)hashtag;
                    hashtags.add(hashtagJson.getString("text"));
                });
        return hashtags;
    }
}
