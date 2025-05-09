package uk.gov.moj.cp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.util.List;

public class Utils {
    public static final ObjectMapper mapper = new ObjectMapper();

    public static JSONObject jsonStringToJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        return json;
    }

    public static <T> T convertJsonStringToObject(String jsonString, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonString, clazz);
    }

    public static <T> List<T> convertJsonStringToList(String jsonString,
                                                      Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(
            jsonString,
            mapper.getTypeFactory().constructCollectionType(List.class, clazz)
        );
    }
}
