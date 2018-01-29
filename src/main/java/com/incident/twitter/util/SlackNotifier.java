package com.incident.twitter.util;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackNotifier {
    private static final Logger logger = LoggerFactory.getLogger(SlackNotifier.class);

    public static void notify(String message) {
        try{
            JSONObject json = new JSONObject();
            json.put("username", "monitor");
            json.put("channel", "#testslack");
            json.put("text", message);
            Request.Post("https://hooks.slack.com/services/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")
                    .bodyString(json.toString(), ContentType.APPLICATION_JSON)
                    .execute();
        }catch (Exception e){
            logger.warn("Failed to send notification {}: {}", message, e.toString());
        }
    }
}
