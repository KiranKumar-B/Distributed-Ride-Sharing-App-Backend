package com.kiran.driversharing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // 1. Makes the KEY "DRIVER_LOCATIONS" readable
        template.setKeySerializer(new StringRedisSerializer());
        
        // 2. Makes the MEMBER "driver-1" readable inside the Geo set
        // Else issues error=> \xac\xed issue, when trying to fetch the value
        template.setValueSerializer(new StringRedisSerializer()); 

        template.afterPropertiesSet();
        return template;
    }
}
