package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class ProsecutionCaseDTOTest {

    @Test
    void testJsonSerialization_allFields() throws JsonProcessingException {
        ProsecutionCaseDTO dto = ProsecutionCaseDTO.builder()
            .caseStatus("Active")
            .reportingRestrictions(true)
            .build();

        assertEquals(
            "{\"caseStatus\":\"Active\",\"reportingRestrictions\":true}",
            objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    void testJsonSerialization_caseStatusOnly() throws JsonProcessingException {
        ProsecutionCaseDTO dto = ProsecutionCaseDTO.builder()
            .caseStatus("Listed")
            .build();

        assertEquals(
            "{\"caseStatus\":\"Listed\",\"reportingRestrictions\":false}",
            objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    void testJsonSerialization_reportingRestrictionsOnly() throws JsonProcessingException {
        ProsecutionCaseDTO dto = ProsecutionCaseDTO.builder()
            .reportingRestrictions(true)
            .build();

        assertEquals(
            "{\"reportingRestrictions\":true}",
            objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    void testJsonSerialization_emptyWhenAllNull() throws JsonProcessingException {
        ProsecutionCaseDTO dto = ProsecutionCaseDTO.builder().build();

        assertEquals("{\"reportingRestrictions\":false}", objectMapper.writeValueAsString(dto));
    }

    @Test
    void testEqualsAndHashCode() {
        ProsecutionCaseDTO a = ProsecutionCaseDTO.builder()
            .caseStatus("Active")
            .reportingRestrictions(true)
            .build();
        ProsecutionCaseDTO b = ProsecutionCaseDTO.builder()
            .caseStatus("Active")
            .reportingRestrictions(true)
            .build();
        ProsecutionCaseDTO differentStatus = ProsecutionCaseDTO.builder()
            .caseStatus("Closed")
            .reportingRestrictions(true)
            .build();
        ProsecutionCaseDTO differentRestrictions = ProsecutionCaseDTO.builder()
            .caseStatus("Active")
            .reportingRestrictions(true)
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, differentStatus);
    }
}
