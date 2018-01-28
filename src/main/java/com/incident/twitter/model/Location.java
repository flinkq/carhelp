package com.incident.twitter.model;

public class Location
{
    private String name;
    private String country;
    private Double latitude;
    private Double longitude;

    public Location(String name, String country, Double latitude, Double longitude)
    {
	setName(name);
	setCountry(country);
	setLatitude(latitude);
	setLongitude(longitude);
    }

    public String getName()
    {
	return name;
    }

    public void setName(String name)
    {
	this.name = name;
    }

    public String getCountry()
    {
	return country;
    }

    public void setCountry(String country)
    {
	this.country = country;
    }

    public Double getLatitude()
    {
	return latitude;
    }

    public void setLatitude(Double latitude)
    {
	this.latitude = latitude;
    }

    public Double getLongitude()
    {
	return longitude;
    }

    public void setLongitude(Double longitude)
    {
	this.longitude = longitude;
    }
}
