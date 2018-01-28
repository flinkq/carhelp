package com.incident.twitter.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Tweet {
    private Instant createdAt;
    private Long id;
    private String text;
    private TwitterProfile twitterProfile;
    private Set<String> hashtags;
    private Location accidentLocaiton;

    public Tweet(Instant createdAt, Long id, String text, TwitterProfile twitterProfile) {
        setCreatedAt(createdAt);
        setId(id);
        setText(text);
        setTwitterProfile(twitterProfile);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public TwitterProfile getTwitterProfile() {
        return twitterProfile;
    }

    public void setTwitterProfile(TwitterProfile twitterProfile) {
        this.twitterProfile = twitterProfile;
    }

    public Set<String> getHashtags() {
        if(hashtags == null){
            hashtags = new HashSet<>();
        }
        return hashtags;
    }

    public void setHashtags(Set<String> hashtags) {
        this.hashtags = hashtags;
    }

    public Optional<Location> getAccidentLocaiton() {
        return Optional.ofNullable(accidentLocaiton);
    }

    public void setAccidentLocaiton(Location accidentLocaiton) {
        this.accidentLocaiton = accidentLocaiton;
    }

    @Override public String toString()
    {
        return "Tweet{" + "createdAt=" + createdAt + ", id=" + id + ", text='" + text + '\'' + ", twitterProfile=" + twitterProfile + ", hashtags="
                        + hashtags + ", accidentLocaiton=" + accidentLocaiton + '}';
    }
}
