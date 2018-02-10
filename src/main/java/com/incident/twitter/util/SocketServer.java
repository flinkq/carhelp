package com.incident.twitter.util;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.incident.twitter.model.GoogleLocation;

public class SocketServer
{

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
        Configuration config = new Configuration();
        config.setHostname("142.44.243.86");
        config.setPort(9092);
        server = new SocketIOServer(config);
        //start server
        server.start();
        //server.stop();
    }

    public static void broadcastAccident(double lat, double lon, String message)
    {
        GoogleLocation object = new GoogleLocation();
        object.setMessage(message);
        object.setLat(lat);
        object.setLon(lon);
        System.out.println("Sending locationEvent");
        try
        {
            getInstance().getBroadcastOperations().sendEvent("locationEvent", object);
        } catch (InterruptedException e)
        {
            System.out.println("Exception sending broadcast");
            e.printStackTrace();
        }
    }
}
