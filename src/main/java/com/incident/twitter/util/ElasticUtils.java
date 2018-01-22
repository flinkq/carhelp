package com.incident.twitter.util;

import com.incident.twitter.sink.SimpleElasticSink;
import org.apache.flink.streaming.connectors.elasticsearch5.ElasticsearchSink;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticUtils
{
    public static ElasticsearchSink getElasticSink(String index, String type) throws UnknownHostException
    {
	Map<String, String> config = new HashMap<>();
	config.put("cluster.name", "test-cluster");
	// This instructs the sink to emit after every element, otherwise they would be buffered
	config.put("bulk.flush.max.actions", "1");
	List<InetSocketAddress> transportAddresses = new ArrayList<>();
	transportAddresses.add(new InetSocketAddress(InetAddress.getByName("ovh"), 9300));
	return new ElasticsearchSink(config, transportAddresses, new SimpleElasticSink(index, type));
    }

}
