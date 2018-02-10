package com.incident.twitter.util;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.incident.twitter.model.GoogleLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class SocketServer implements Serializable
{
    static Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);
    private static SocketIOServer server;
    private SocketServer(){}

    static SocketIOServer getInstance() throws InterruptedException
    {
        if(server == null)
        {
            synchronized (SocketServer.class)
            {
                if(server == null)
                {
                    init();
                }
            }
        }
        return server;
    }

    public static void init() throws InterruptedException
    {
	LOGGER.info("Starting socket Server");
        Configuration config = new Configuration();
        //config.setHostname("142.44.243.86");
        config.setPort(9092);
        config.setOrigin("*");
        server = new SocketIOServer(config);
        //start server
        server.start();
	LOGGER.info("Socket Server Started");
	//server.stop();
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
            getInstance().getBroadcastOperations().sendEvent("locationEvent", object);
	    LOGGER.info("location sent");
        } catch (Exception e)
        {
	    LOGGER.error("Exception sending broadcast", e);
        }
    }
}
