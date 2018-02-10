package com.incident.twitter.sink;

import com.incident.twitter.model.Tweet;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.slf4j.LoggerFactory;

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
	return Requests.indexRequest()
			.index(index)
            		.type(type)
                    .id(element.getId().toString())
            		.source(element.toString());
    }

    @Override public void process(Tweet element, RuntimeContext ctx, RequestIndexer indexer)
    {
	indexer.add(createIndexRequest(element));
    }

}

