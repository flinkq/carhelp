package com.incident.twitter.service.impl;

import com.incident.twitter.model.Location;
import com.incident.twitter.service.LocationService;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

public class GoogleLocationService implements LocationService{
    private static GoogleLocationService googleLocationService;

    private GoogleLocationService() {
        LoggerFactory.getLogger(this.getClass())
                .info("INIT CLASS");
    }
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
