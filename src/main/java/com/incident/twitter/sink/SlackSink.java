package com.incident.twitter.sink;

import com.incident.twitter.model.Tweet;
import com.incident.twitter.util.SlackNotifier;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

public class SlackSink extends RichSinkFunction<Tweet>
{
    @Override public void invoke(Tweet entity) throws Exception
    {
	entity.getAccidentLocaiton().ifPresent(location -> {
	    SlackNotifier.notify("Detected at " + location.getName());
	    try(Jedis jedis = new Jedis())
	    {
		JSONObject json = new JSONObject();
		json.put("lat", location.getLatitude());
		json.put("lon", location.getLongitude());
		json.put("message", location.getName());
		jedis.publish("location", json.toString());
		//SocketServer.broadcastAccident(location.getLatitude(), location.getLongitude(), location.getName());
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
