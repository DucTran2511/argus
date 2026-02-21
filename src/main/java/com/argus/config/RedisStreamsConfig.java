package com.argus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;

import com.argus.core.constant.StreamKeys;
import com.argus.infra.stream.consumer.TransactionStreamConsumer;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.util.ErrorHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@EnableRedisRepositories
public class RedisStreamsConfig {
    @Bean
    public RedisTemplate<String, Object> streamRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory factory) {

        var options = StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .errorHandler(t -> log.error("Redis Stream Error: {}", t.getMessage()))
                .build();

        return StreamMessageListenerContainer.create(factory, options);
    }

    @Bean
    public Subscription transactionSubscription(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            TransactionStreamConsumer consumer) {

        var readOptions = StreamReadRequest.builder(
                StreamOffset.create(StreamKeys.TRANSACTION_STREAM, ReadOffset.lastConsumed()))
                .consumer(Consumer.from(StreamKeys.TX_PROCESSOR_GROUP,
                        StreamKeys.TX_PROCESSOR_CONSUMER + "-" + UUID.randomUUID()))
                .autoAcknowledge(false)
                .build();

        container.start();
        return container.register(readOptions, consumer);
    }
}
