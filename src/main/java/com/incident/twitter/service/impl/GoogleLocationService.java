package com.incident.twitter.service.impl;

import com.incident.twitter.model.Location;
import com.incident.twitter.service.LocationService;

import java.util.Collection;
import java.util.Optional;

public class GoogleLocationService implements LocationService{
    private static GoogleLocationService googleLocationService;

    private GoogleLocationService() { }
    public static GoogleLocationService getInstance(){
        if(googleLocationService == null){
            googleLocationService = new GoogleLocationService();
        }
        return googleLocationService;
    }
    @Override
    public Optional<Location> detectLocation(Collection<String> hashtags) {
        return Optional.empty();
    }
}
