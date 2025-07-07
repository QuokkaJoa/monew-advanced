package com.part2.monew.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Lettuce를 이용한 기본 Redis 연결 설정
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(cfg);
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        // Redisson 분산 락, Pub/Sub 등을 위해 사용
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }
}