package app.meucarrinho.adapter.idempotency.redis;

import app.meucarrinho.application.common.port.IdempotencyStore;
import app.meucarrinho.application.common.port.IdempotencyTtl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Selected alone by {@code carrinho.adapters.idempotency=redis}. With {@code redis-postgres}, bootstrap's composite
 * builds this store through {@link #create} instead, so it never becomes a second port bean (ADR 0010).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "carrinho.adapters", name = "idempotency", havingValue = "redis")
public class RedisIdempotencyConfiguration {
    @Bean
    IdempotencyStore redisIdempotencyStore(RedisConnectionFactory connections) {
        return create(connections, IdempotencyTtl.STANDARD);
    }

    /** Builds the Redis store on Boot's connection factory without registering it as a bean. */
    public static IdempotencyStore create(RedisConnectionFactory connections, IdempotencyTtl ttl) {
        RedisTemplate<String, byte[]> redis = new RedisTemplate<>();
        redis.setConnectionFactory(connections);
        redis.setKeySerializer(RedisSerializer.string());
        redis.setValueSerializer(RedisSerializer.byteArray());
        redis.afterPropertiesSet();
        return new RedisIdempotencyStore(redis, ttl);
    }
}
