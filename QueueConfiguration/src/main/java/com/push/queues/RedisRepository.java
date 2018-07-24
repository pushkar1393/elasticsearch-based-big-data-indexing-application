package com.push.queues;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisRepository {

	@Bean
	public static JedisPool getJedisPool() {
		JedisPool jedisPool = new JedisPool("localhost", 6379);
		return jedisPool;
	}
	
	@Bean
	public static Jedis getJedis() {
		Jedis jedis = getJedisPool().getResource();
		return jedis;
	}
}
