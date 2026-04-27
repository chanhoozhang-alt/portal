package com.xxx.portal.common.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置，使用 fastjson2 序列化
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Fastjson2RedisSerializer<Object> serializer = new Fastjson2RedisSerializer<>(Object.class);

        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    static class Fastjson2RedisSerializer<T> implements RedisSerializer<T> {

        private static final JSONReader.AutoTypeBeforeHandler AUTO_TYPE_FILTER =
                JSONReader.autoTypeFilter("com.xxx.portal.");

        private final Class<T> clazz;

        Fastjson2RedisSerializer(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public byte[] serialize(T t) throws SerializationException {
            if (t == null) {
                return new byte[0];
            }
            return JSON.toJSONBytes(t, JSONWriter.Feature.WriteClassName);
        }

        @Override
        public T deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return (T) JSON.parseObject(bytes, clazz, AUTO_TYPE_FILTER,
                    JSONReader.Feature.SupportAutoType);
        }
    }
}
