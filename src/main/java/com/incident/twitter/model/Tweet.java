package com.incident.twitter.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Tweet
{
    private Date createdAt;
    private Long id;
    private String text;
    private TwitterProfile twitterProfile;
    private Set<String> hashtags;
    private Location accidentLocaiton;

    public Tweet(Date createdAt, Long id, String text, TwitterProfile twitterProfile)
    {
	setCreatedAt(createdAt);
	setId(id);
	setText(text);
	setTwitterProfile(twitterProfile);
    }

    public Date getCreatedAt()
    {
	return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
	this.createdAt = createdAt;
    }

    public Long getId()
    {
	return id;
    }

    public void setId(Long id)
    {
	this.id = id;
    }

    public String getText()
    {
	return text;
    }

    public void setText(String text)
    {
	this.text = text;
    }

    public TwitterProfile getTwitterProfile()
    {
	return twitterProfile;
    }

    public void setTwitterProfile(TwitterProfile twitterProfile)
    {
	this.twitterProfile = twitterProfile;
    }

    public Set<String> getHashtags()
    {
	if (hashtags == null)
	{
	    hashtags = new HashSet<>();
	}
	return hashtags;
    }

    public void setHashtags(Set<String> hashtags)
    {
	this.hashtags = hashtags;
    }

    public Optional<Location> getAccidentLocaiton()
    {
	return Optional.ofNullable(accidentLocaiton);
    }

    public void setAccidentLocaiton(Location accidentLocaiton)
    {
	this.accidentLocaiton = accidentLocaiton;
    }

    @Override
    public String toString()
    {
	try
	{
	    return new ObjectMapper().writeValueAsString(this);
	} catch (JsonProcessingException e)
	{
	    e.printStackTrace();
	    return "Tweet{" + "createdAt=" + createdAt + ", id=" + id + ", text='" + text + '\'' + ", twitterProfile=" + twitterProfile + ", hashtags="
			    + hashtags + ", accidentLocaiton=" + accidentLocaiton + '}';
	}
    }
}
