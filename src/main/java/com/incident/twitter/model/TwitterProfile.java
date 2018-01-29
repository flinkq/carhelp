package com.incident.twitter.model;

public class TwitterProfile {
    private Long id;
    private String handle;
    private String country;

    public TwitterProfile(Long id, String handle, String country) {
        setId(id);
        setHandle(handle);
        setCountry(country);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
