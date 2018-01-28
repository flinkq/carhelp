package com.incident.twitter.core;

import com.google.gson.GsonBuilder;
import com.google.maps.errors.ApiException;
import com.incident.twitter.service.impl.GoogleLocationService;

import java.io.IOException;

public class TestMaps
{
    public static void main(String[] args) throws InterruptedException, ApiException, IOException
    {
//        GeoApiContext context = new GeoApiContext.Builder()
//                        .apiKey("AIzaSyD-IsobBghjtWs6N7dv9s8iip9ZBpTLGek")
//                        .build();
//        GeocodingResult[] results =  GeocodingApi.geocode(context,
//                        "1600 Amphitheatre Parkway Mountain View, CA 94043").await();
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        System.out.println(gson.toJson(results[0]));
        GoogleLocationService googleLocationService = GoogleLocationService.getInstance();
        googleLocationService.detectLocation("كفانا_بقى").ifPresent(location -> {
            String result = new GsonBuilder().create().toJson(location);
            System.out.println(result);
        });
    }
}
