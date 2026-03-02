package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CourtSittingDtoTest {

    @Test
    void testJsonInclude() throws JsonProcessingException {
        CourtSittingDto s1 = CourtSittingDto.builder()
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .judiciaryId("J001")
            .courtHouse("London Court")
            .courtRoom("Room 1")
            .build();
        CourtSittingDto s2 = CourtSittingDto.builder().build();

        assertEquals(
            "{\"sittingStart\":\"2025-01-01T09:00\",\"sittingEnd\":\"2025-01-01T17:00\",\"judiciaryId\":\"J001\",\"courtHouse\":\"London Court\",\"courtRoom\":\"Room 1\"}",
            objectMapper.writeValueAsString(s1)
        );
        assertEquals("{\"sittingStart\":null,\"sittingEnd\":null,\"judiciaryId\":null,\"courtHouse\":null,\"courtRoom\":null}", objectMapper.writeValueAsString(s2));
    }

    @Test
    void testBuilderAndEquals() {
        CourtSittingDto s1 = CourtSittingDto.builder()
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .judiciaryId("J001")
            .courtHouse("London Court")
            .courtRoom("Room 1")
            .build();
        CourtSittingDto s2 = CourtSittingDto.builder()
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .judiciaryId("J001")
            .courtHouse("London Court")
            .courtRoom("Room 1")
            .build();

        assertEquals(s1.getSittingStart(), s2.getSittingStart());
        assertEquals(s1.getSittingEnd(), s2.getSittingEnd());
        assertEquals(s1.getJudiciaryId(), s2.getJudiciaryId());
        assertEquals(s1.getCourtHouse(), s2.getCourtHouse());
        assertEquals(s1.getCourtRoom(), s2.getCourtRoom());

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }
}
