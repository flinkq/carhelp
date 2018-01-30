package com.incident.twitter.filter;

import com.incident.twitter.model.Tweet;
import org.apache.flink.api.common.functions.FilterFunction;

public class TMCAccidentsFilter implements FilterFunction<Tweet>
{
    @Override public boolean filter(Tweet tweet) throws Exception
    {
	return (tweet.getHashtags().contains("كفانا_بقى"))
			|| tweet.getText().contains("كفانا_بقى")
			|| tweet.getText().contains("تصادم")
			|| tweet.getText().contains("حادث");
    }
}
