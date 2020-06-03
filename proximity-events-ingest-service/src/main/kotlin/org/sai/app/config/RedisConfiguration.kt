package org.sai.app.config

import io.lettuce.core.ClientOptions
import io.lettuce.core.TimeoutOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.*
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import java.time.Duration


@Configuration
class RedisConfiguration(@Value("\${redis.host}") private val host: String,
                         @Value("\${redis.port}") private val port: Int,
                         @Value("\${redis.database}") private val database: Int,
                         @Value("\${redis.password}") private val password: String) {

    @Bean
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val redisConfiguration = RedisStandaloneConfiguration()
        redisConfiguration.database = database
        redisConfiguration.hostName = host
        redisConfiguration.port = port
        redisConfiguration.password = RedisPassword.of(password)
        val clientOptions = ClientOptions.builder()
                .autoReconnect(true)
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofSeconds(10)).build())
                .requestQueueSize(Integer.MAX_VALUE)
                .build()
        val clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(10))
                .clientOptions(clientOptions)
                .build()

        return LettuceConnectionFactory(redisConfiguration, clientConfiguration)
    }

    @Bean
    fun keyCommands(reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory): ReactiveKeyCommands {
        return reactiveRedisConnectionFactory.reactiveConnection
                .keyCommands()
    }

    @Bean
    fun hyperLogLogCommands(reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory): ReactiveHyperLogLogCommands {
        return reactiveRedisConnectionFactory.reactiveConnection
                .hyperLogLogCommands()
    }

    @Bean
    fun stringCommands(reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory): ReactiveStringCommands {
        return reactiveRedisConnectionFactory.reactiveConnection
                .stringCommands()
    }
}