package kz.whosnext.hr.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

@NullMarked
public class JsonRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public JsonRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(@Nullable Object value) throws SerializationException {
        if (value == null) return new byte[0];
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Error serializing object to JSON", e);
        }
    }

    @Override
    public @Nullable Object deserialize(byte @Nullable [] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return objectMapper.readValue(bytes, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing JSON to object", e);
        }
    }
}

