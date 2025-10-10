package uk.gov.moj.cp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;

public class Utils {
    public static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static <T> T convertJsonStringToObject(String jsonString, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, clazz);
    }

    public static <T> List<T> convertJsonStringToList(String jsonString,
                                                      Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(
            jsonString,
            objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
        );
    }

    public static <K, V> Map<K, V> convertJsonStringToMap(String jsonString, Class<K> keyClass,
                                                          Class<V> valueClass) throws JsonProcessingException {
        return convertJsonStringToType(
            jsonString,
            objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass)
        );
    }

    public static <T> T convertJsonStringToType(String jsonString,
                                                JavaType javaType) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, javaType);
    }

    public static <T> T convertJsonStringToType(String jsonString, Class<T> clazz)
        throws JsonProcessingException {
        return objectMapper.readValue(jsonString, clazz);
    }

    public static JsonNode getJsonNode(String jsonString, String key)
        throws JsonProcessingException {
        return objectMapper.readTree(jsonString).get(key);
    }
}
