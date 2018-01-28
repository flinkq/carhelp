package com.incident.twitter.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.incident.twitter.model.Location;
import com.incident.twitter.service.LocationService;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GoogleLocationService implements LocationService
{

    Logger logger = Logger.getLogger(this.getClass());

    private static GoogleLocationService helper;
    private String apiKey;
    private String queueName;
    private ScheduledExecutorService scheduler;

    private GoogleLocationService(ParameterTool params)
    {
//	init(params);
    }

    public static GoogleLocationService getInstance()
    {
	if (helper == null)
	{
	    synchronized (GoogleLocationService.class)
	    {
		if (helper == null)
		{
		    helper = new GoogleLocationService(null);
		}
	    }
	}
	return helper;
    }

    private void init(ParameterTool params)
    {
	this.scheduler = Executors.newScheduledThreadPool(1);
	BalanceResetTask task = new BalanceResetTask(params.get(""), params.get(""), params.getLong(""));
	scheduler.scheduleAtFixedRate(task, 0, 24, TimeUnit.HOURS);
    }

    @Override public Optional<Location> detectLocation(String hashtag)
    {
        logger.info("Detecting location for " + hashtag);
        if(isLocation(hashtag))
	{
	    String location = getFromCache(hashtag);
	    if (location == null)
	    {
		logger.info(hashtag + " not found in cache... calling google");
		GeoApiContext context = new GeoApiContext.Builder().apiKey("AIzaSyD-IsobBghjtWs6N7dv9s8iip9ZBpTLGek").build();
		GeocodingResult[] results = new GeocodingResult[0];
		try
		{
		    results = GeocodingApi.geocode(context, hashtag).language("ar").await();
		    Gson gson = new GsonBuilder().setPrettyPrinting().create();
		    location = gson.toJson(results[0]);
		    logger.trace("Got response for " + hashtag + " " + results[0]);
		    setInCache(hashtag, location);
		} catch (Exception exc)
		{
		    addBadLocation(hashtag);
		    logger.error("Could not get google response for " + hashtag + ": " + exc.toString());
		    return Optional.empty();
		}
	    } else
	    {
		logger.info("Found " + hashtag + " in cache");
	    }
	    try
	    {
		return Optional.of(toLocation(location, hashtag));
	    } catch (JSONException e)
	    {
	        //addBadLocation(hashtag);
		logger.error("Could not parse google location response for " + hashtag + " : " + location, e);
	    }
	}else
	{
	    logger.info("Already know that " + hashtag + " in not a location");
	}
	return Optional.empty();
    }

    private Location toLocation(String response, String hashtag)
    {
        logger.debug("Parsing location response " + response);
	JSONObject locationJson = new JSONObject(response);
	String country = "";
	Double longitude;
	Double latitude;
	for (Object addressComponent : locationJson.getJSONArray("addressComponents") ){
	JSONObject addressComponentJson = (JSONObject) addressComponent;
	List<Object> types = addressComponentJson.getJSONArray("types").toList();
	if (types.stream().anyMatch(type -> type.equals("COUNTRY")))
	{
	    country = addressComponentJson.getString("longName");
	}
    }
    JSONObject geometryLocation = locationJson.getJSONObject("geometry")
		    .getJSONObject("location");
    longitude = geometryLocation
		    .getDouble("lng");
    latitude = geometryLocation.getDouble("lat");
    return new Location(hashtag, country, latitude, longitude);
    }

    private boolean isLocation(String hashtag)
    {
	logger.debug("Checking if " + hashtag + " is valid");
	boolean isLocation = true;
	try (Jedis jedis = new Jedis())
	{
	    isLocation = !jedis.sismember("cache:notlocations", hashtag);
	} catch (Exception exc)
	{
	    exc.printStackTrace();
	}
	if(!isLocation){
	    logger.debug(hashtag + " is not a valid location");
	}
	return isLocation;
    }

    private void addBadLocation(String hashtag)
    {
	try (Jedis jedis = new Jedis())
	{
	    jedis.sadd("cache:notlocations", hashtag);
	} catch (Exception exc)
	{
	    exc.printStackTrace();
	}
    }

    private void setInCache(String hashtag, String location)
    {
        logger.debug("Adding to cache " + hashtag);
	try (Jedis jedis = new Jedis())
	{
	    jedis.hset("cache:location", hashtag, location);
	} catch (Exception exc)
	{
	    exc.printStackTrace();
	}
    }

    private String getFromCache(String hashtag)
    {
	try (Jedis jedis = new Jedis())
	{
	    return jedis.hget("cache:location", hashtag);
	} catch (Exception exc)
	{
	    exc.printStackTrace();
	}
	return null;
    }

    static class BalanceResetTask implements Runnable
    {
	Logger logger = Logger.getLogger(BalanceResetTask.class);

	String queue;
	long balance = 0;
	String host;

	public BalanceResetTask(String host, String queue, long balance)
	{
	    this.queue = queue;
	    this.balance = balance;
	    this.host = host;
	}

	@Override public void run()
	{
	    logger.trace("Before Reset - " + queue);
	    new Jedis(host).set(queue, String.valueOf(balance));
	    logger.trace("After Reset - " + queue);
	}
    }

}
