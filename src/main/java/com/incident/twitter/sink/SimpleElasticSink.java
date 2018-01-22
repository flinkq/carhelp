package com.incident.twitter.sink;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.util.Map;

public class SimpleElasticSink implements ElasticsearchSinkFunction<Map>
{
    String index;
    String type;

    public SimpleElasticSink(String index, String type)
    {
	this.index = index;
	this.type = type;
    }

    public IndexRequest createIndexRequest(Map element)
    {
	return Requests.indexRequest().index(index).type(type).source(element);
    }

    @Override public void process(Map element, RuntimeContext ctx, RequestIndexer indexer)
    {
	indexer.add(createIndexRequest(element));
    }

}

