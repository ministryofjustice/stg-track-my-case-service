package uk.gov.moj.cp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class Utils {
    public static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static <T> List<T> convertJsonStringToList(String jsonString,
                                                      Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(
            jsonString,
            objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
        );
    }
}
