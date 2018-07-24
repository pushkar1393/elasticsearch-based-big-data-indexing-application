package com.push.assignment.config;

import redis.clients.jedis.Jedis;

public class QueueConfigurer {
	
	private final String waitQueue = "TestSample:waitQueue";
	private final String workQueue = "TestSample:workQueue";

	Jedis instance = null;

	 public QueueConfigurer(Jedis jedis) {
		this.instance = jedis;
	}

	public boolean sendJobToWaitQueue(String uri) {
		try {
			instance.lpush(waitQueue, uri);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
