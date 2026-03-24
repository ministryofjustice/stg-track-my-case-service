package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CaseStatusDtoTest {

    @Test
    void testJsonSerialization_allFields() throws JsonProcessingException {
        CaseStatusDto dto = CaseStatusDto.builder()
            .caseStatus("Active")
            .reportingRestrictions("Reporting restrictions apply")
            .build();

        assertEquals(
            "{\"caseStatus\":\"Active\",\"reportingRestrictions\":\"Reporting restrictions apply\"}",
            objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    void testJsonSerialization_caseStatusOnly() throws JsonProcessingException {
        CaseStatusDto dto = CaseStatusDto.builder()
            .caseStatus("Listed")
            .build();

        assertEquals(
            "{\"caseStatus\":\"Listed\"}",
            objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    void testJsonSerialization_reportingRestrictionsOnly() throws JsonProcessingException {
        CaseStatusDto dto = CaseStatusDto.builder()
            .reportingRestrictions("Anonymity order in place")
            .build();

        assertEquals(
            "{\"reportingRestrictions\":\"Anonymity order in place\"}",
            objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    void testJsonSerialization_emptyWhenAllNull() throws JsonProcessingException {
        CaseStatusDto dto = CaseStatusDto.builder().build();

        assertEquals("{}", objectMapper.writeValueAsString(dto));
    }

    @Test
    void testEqualsAndHashCode() {
        CaseStatusDto a = CaseStatusDto.builder()
            .caseStatus("Active")
            .reportingRestrictions("None")
            .build();
        CaseStatusDto b = CaseStatusDto.builder()
            .caseStatus("Active")
            .reportingRestrictions("None")
            .build();
        CaseStatusDto differentStatus = CaseStatusDto.builder()
            .caseStatus("Closed")
            .reportingRestrictions("None")
            .build();
        CaseStatusDto differentRestrictions = CaseStatusDto.builder()
            .caseStatus("Active")
            .reportingRestrictions("Restricted")
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, differentStatus);
        assertNotEquals(a, differentRestrictions);
    }
}
