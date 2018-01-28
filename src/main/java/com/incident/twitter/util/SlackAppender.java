package com.incident.twitter.util;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;

public class SlackAppender
{
    /**
     * Append the following message to slack.
     *
     * @param message
     */
    public static void append(String message)
    {
	try
	{
	    JSONObject json = new JSONObject();
	    json.put("username", "bot");
	    json.put("channel", "#testslack");
	    json.put("text", message);
	    Request.Post("https://hooks.slack.com/services/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")
			    .bodyString(json.toString(), ContentType.APPLICATION_JSON)
			    .execute();
	} catch (Exception e)
	{
	    e.printStackTrace();
	}
    }
}

