package com.push.assignment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfiguration {


	@Bean
	public static JedisPool getConn() {
		return new JedisPool("localhost", 6379);

	}

	@Bean
	public static Jedis getJedis() {
		return getConn().getResource();
	}

}
