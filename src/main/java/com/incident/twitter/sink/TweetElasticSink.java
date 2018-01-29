package com.incident.twitter.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.twitter.model.Tweet;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TweetElasticSink implements ElasticsearchSinkFunction<Tweet>
{
    String index;
    String type;

    public TweetElasticSink(String index, String type)
    {
	this.index = index;
	this.type = type;
    }

    public IndexRequest createIndexRequest(Tweet element)
    {
        LoggerFactory.getLogger(this.getClass())
                .info("INDEXING: {}", element);
        Map data = new ObjectMapper().convertValue(element, Map.class);
	return Requests.indexRequest()
			.index(index)
            		.type(type)
            		.source(data);
    }

    @Override public void process(Tweet element, RuntimeContext ctx, RequestIndexer indexer)
    {
	indexer.add(createIndexRequest(element));
    }

}

