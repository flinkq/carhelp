package com.incident.twitter.model;

public class GoogleLocation
{
    private String message;
    private double lat;
    private double lon;

    public GoogleLocation()
    {
    }

    public String getMessage()
    {
	return message;
    }

    public void setMessage(String message)
    {
	this.message = message;
    }

    public double getLat()
    {
	return lat;
    }

    public void setLat(double lat)
    {
	this.lat = lat;
    }

    public double getLon()
    {
	return lon;
    }

    public void setLon(double lon)
    {
        this.lon = lon;
    }

}
