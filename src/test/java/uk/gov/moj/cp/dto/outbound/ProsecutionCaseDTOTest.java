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
            .caseStatus(CaseStatus.ACTIVE)
            .reportingRestrictions(true)
            .build();

        assertEquals(
            "{\"caseStatus\":\"ACTIVE\",\"reportingRestrictions\":true}",
            objectMapper.writeValueAsString(dto)
        );
    }

    @Test
    void testJsonSerialization_caseStatusOnly() throws JsonProcessingException {
        assertEquals(
            "{\"caseStatus\":\"SJP_REFERRAL\",\"reportingRestrictions\":false}",
            objectMapper.writeValueAsString(ProsecutionCaseDTO.builder()
                .caseStatus("SJP_REFERRAL")
                .build())
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
        ProsecutionCaseDTO active1 = ProsecutionCaseDTO.builder()
            .caseStatus(CaseStatus.ACTIVE)
            .reportingRestrictions(true)
            .build();
        ProsecutionCaseDTO active2 = ProsecutionCaseDTO.builder()
            .caseStatus(CaseStatus.ACTIVE)
            .reportingRestrictions(true)
            .build();
        ProsecutionCaseDTO inactive = ProsecutionCaseDTO.builder()
            .caseStatus(CaseStatus.INACTIVE)
            .reportingRestrictions(true)
            .build();
        ProsecutionCaseDTO differentRestrictions = ProsecutionCaseDTO.builder()
            .caseStatus(CaseStatus.ACTIVE)
            .reportingRestrictions(false)
            .build();

        assertEquals(active1, active2);
        assertEquals(active1.hashCode(), active2.hashCode());
        assertNotEquals(active1, inactive);
        assertNotEquals(active1, differentRestrictions);
    }
}
