package com.incident.twitter.service;

import com.incident.twitter.model.Location;

import java.util.Optional;

public interface LocationService
{
    Optional<Location> detectLocation(String hashtag);
}
