package com.incident.twitter.dto;

import java.util.List;

public class GoogleLocationResponse {

    private String longName;
    private String shortName;
    private List<String> types = null;

    public String getLongName() {
	return longName;
    }

    public void setLongName(String longName) {
	this.longName = longName;
    }

    public String getShortName() {
	return shortName;
    }

    public void setShortName(String shortName) {
	this.shortName = shortName;
    }

    public List<String> getTypes() {
	return types;
    }

    public void setTypes(List<String> types) {
	this.types = types;
    }
}