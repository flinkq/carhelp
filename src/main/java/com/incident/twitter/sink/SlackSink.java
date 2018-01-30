package com.incident.twitter.sink;

import com.incident.twitter.model.Tweet;
import com.incident.twitter.util.SlackNotifier;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

public class SlackSink extends RichSinkFunction<Tweet>
{
    @Override public void invoke(Tweet entity) throws Exception
    {
	entity.getAccidentLocaiton().ifPresent(location -> {
	    SlackNotifier.notify("Detected " + entity.getId() + " at " + location.getName());
	    try
	    {
		//TwilioNotifier.notify("Detected accident at " + location.getName() + ", " + location.getCountry());
	    } catch (Exception e)
	    {
	    }
	});
    }

    @Override
    public void open(Configuration parameters) throws Exception
    {
	//TODO read slack url
    }

    @Override
    public void close() throws Exception
    {

    }
}

/*
location -> {

			    }
 */
