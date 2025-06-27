package uk.gov.moj.cp.pact.helper;

import au.com.dius.pact.consumer.dsl.LambdaDslJsonBody;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.LambdaDslObject;
import au.com.dius.pact.consumer.dsl.DslPart;
import com.fasterxml.jackson.databind.JsonNode;

public class PactDslHelper {

    public static DslPart fromJson(JsonNode jsonNode) {
        return LambdaDsl.newJsonBody(dsl -> buildJson(dsl, jsonNode)).build();
    }

    public static void buildJson(LambdaDslJsonBody dsl, JsonNode json) {
        json.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (value.isTextual()) {
                dsl.stringType(key, value.textValue());
            } else if (value.isNumber()) {
                dsl.numberType(key, value.numberValue());
            } else if (value.isBoolean()) {
                dsl.booleanType(key, value.booleanValue());
            } else if (value.isObject()) {
                dsl.object(key, obj -> buildJson(obj, value));
            } else if (value.isArray()) {
                if (value.size() > 0 && value.get(0).isObject()) {
                    dsl.minArrayLike(key, 1, arr -> buildJson(arr, value.get(0)));
                } else {
                    dsl.array(key, arr -> {
                        for (JsonNode item : value) {
                            if (item.isTextual()) {
                                arr.stringValue(item.asText());
                            } else if (item.isNumber()) {
                                arr.numberValue(item.numberValue());
                            } else if (item.isBoolean()) {
                                arr.booleanValue(item.booleanValue());
                            } else {
                                arr.stringValue(item.toString());
                            }
                        }
                    });
                }
            } else {
                dsl.stringType(key, "UNKNOWN");
            }
        });
    }

    public static void buildJson(LambdaDslObject dsl, JsonNode json) {
        json.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (value.isTextual()) {
                dsl.stringType(key, value.textValue());
            } else if (value.isNumber()) {
                dsl.numberType(key, value.numberValue());
            } else if (value.isBoolean()) {
                dsl.booleanType(key, value.booleanValue());
            } else if (value.isObject()) {
                dsl.object(key, obj -> buildJson(obj, value));
            } else if (value.isArray()) {
                if (value.size() > 0 && value.get(0).isObject()) {
                    dsl.minArrayLike(key, 1, arr -> buildJson(arr, value.get(0)));
                } else {
                    dsl.array(key, arr -> {
                        for (JsonNode item : value) {
                            if (item.isTextual()) {
                                arr.stringValue(item.asText());
                            } else if (item.isNumber()) {
                                arr.numberValue(item.numberValue());
                            } else if (item.isBoolean()) {
                                arr.booleanValue(item.booleanValue());
                            } else {
                                arr.stringValue(item.toString());
                            }
                        }
                    });
                }
            } else {
                dsl.stringType(key, "UNKNOWN");
            }
        });
    }


}
