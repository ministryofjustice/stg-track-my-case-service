package uk.gov.moj.cp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.moj.generated.hmcts.CaseresultsSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UtilsTest {

    @Test
    void testConvertJsonStringToList_ValidJson() throws JsonProcessingException {
        String jsonString = "[{\"name\":\"John\"}, {\"name\":\"Jane\"}]";

        List<Person> result = Utils.convertJsonStringToList(jsonString, Person.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).name);
        assertEquals("Jane", result.get(1).name);
    }

    @Test
    void testConvertJsonStringToListOfCaseresult_ValidJson() throws JsonProcessingException {
        String jsonString = "[ { \"resultText\": \"Guilty plea accepted by the court.\" }, "
            + "{ \"resultText\": \"Sentenced to 12 months custody.\" }, "
            + "{ \"resultText\": \"Fine of £500 imposed.\" } ]";


        List<CaseresultsSchema> result = Utils.convertJsonStringToList(jsonString, CaseresultsSchema.class);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Guilty plea accepted by the court.", result.get(0).getResultText());
        assertEquals("Sentenced to 12 months custody.", result.get(1).getResultText());
    }


    @Test
    void testConvertJsonStringToList_EmptyJsonArray() throws JsonProcessingException {
        String jsonString = "[]";

        List<Person> result = Utils.convertJsonStringToList(jsonString, Person.class);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertJsonStringToList_InvalidJson() {
        String jsonString = "[{\"name\":\"John\", {\"name\":\"Jane\"}]";

        assertThrows(
            JsonProcessingException.class, () -> {
                Utils.convertJsonStringToList(jsonString, Person.class);
            }
        );
    }

    static class Person {
        public String name;
    }

}

