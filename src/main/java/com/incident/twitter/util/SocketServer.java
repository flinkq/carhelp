package com.incident.twitter.util;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.incident.twitter.model.GoogleLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer implements Serializable
{
    static Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);
    private static SocketIOServer server;

    private SocketServer()
    {
    }

    static Map<String, GoogleLocation> lastThree;
    final static String activeUsersMap = "active:users";

    static
    {
	lastThree = new ConcurrentHashMap<>(3);
	lastThree.put("first", new GoogleLocation());
	lastThree.put("second", new GoogleLocation());
	lastThree.put("third", new GoogleLocation());
    }

    static SocketIOServer getInstance() throws InterruptedException
    {
	if (server == null)
	{
	    synchronized (SocketServer.class)
	    {
		if (server == null)
		{
		    init();
		}
	    }
	}
	return server;
    }

    public static void init() throws InterruptedException
    {
	LOGGER.warn("Starting socket Server");
	Configuration config = new Configuration();
	config.setPort(9092);

	server = new SocketIOServer(config);
	server.addConnectListener(new ConnectListener()
	{
	    @Override public void onConnect(SocketIOClient socketIOClient)
	    {
		addUser(socketIOClient);
		CompletableFuture.runAsync(() -> {
		    for (Map.Entry<String, GoogleLocation> entry : lastThree.entrySet())
		    {
			if (entry.getValue() != null && entry.getValue().getMessage() != null)
			{
			    try
			    {
				Thread.sleep(4000); //sleep 4000 seconds before sending a new one
			    } catch (InterruptedException e)
			    {
				LOGGER.warn(Thread.currentThread().getName() + " was interrupted!");
			    }
			    socketIOClient.sendEvent("locationEvent", entry.getValue());
			}
		    }
		});

	    }
	});

	server.addDisconnectListener(new DisconnectListener()
	{
	    @Override public void onDisconnect(SocketIOClient socketIOClient)
	    {
		removeUser(socketIOClient);
	    }
	});
	server.start();
	LOGGER.info("Socket Server Started");
	//server.stop();
	Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	    LOGGER.info("Entered the shutdown hook");
	    server.stop();
	}));
    }

    public static void broadcastAccident(double lat, double lon, String message)
    {
	GoogleLocation object = new GoogleLocation();
	object.setMessage(message);
	object.setLat(lat);
	object.setLon(lon);
	LOGGER.info("Sending locationEvent");
	try
	{
	    switchLastThree(object);
	    getInstance().getBroadcastOperations().sendEvent("locationEvent", object);
	    LOGGER.info("location sent");
	} catch (Exception e)
	{
	    LOGGER.error("Exception sending broadcast", e);
	}
    }

    private static void switchLastThree(GoogleLocation obj)
    {
	lastThree.put("third", lastThree.get("second"));
	lastThree.put("second", lastThree.get("first"));
	lastThree.put("first", obj);
    }

    private static void addUser(SocketIOClient user)
    {
	try (Jedis jedis = new Jedis())
	{
	    jedis.hset(activeUsersMap + "_permanent", user.getRemoteAddress().toString(), new Date().toString());
	    jedis.hset(activeUsersMap, user.getRemoteAddress().toString(), new Date().toString());
	} catch (Exception exc)
	{
	    LOGGER.error("Error adding user to map of users", exc);
	}
    }

    private static void removeUser(SocketIOClient user)
    {
	try (Jedis jedis = new Jedis())
	{
	    jedis.hset(activeUsersMap, user.getRemoteAddress().toString(), new Date().toString());
	} catch (Exception exc)
	{
	    LOGGER.error("Error adding user to map of users", exc);
	}
    }
}
