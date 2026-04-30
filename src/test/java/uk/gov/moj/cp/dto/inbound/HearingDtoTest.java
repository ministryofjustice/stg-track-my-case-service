package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class HearingDtoTest {

    @Test
    void testJsonInclude() throws JsonProcessingException {
        WeekCommencingDto weekCommencing = WeekCommencingDto.builder()
            .courtHouse("London Court")
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .build();
        CourtSittingDto sitting1 = CourtSittingDto.builder()
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .judiciaryId("J001")
            .courtHouse("London Court")
            .courtRoom("Room 1")
            .build();
        CourtSittingDto sitting2 = CourtSittingDto.builder()
            .sittingStart("2025-01-02T09:00")
            .sittingEnd("2025-01-02T17:00")
            .judiciaryId("J002")
            .courtHouse("London Court")
            .courtRoom("Room 2")
            .build();

        HearingDto h1 = HearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main hearing")
            .listNote("List note")
            .weekCommencing(weekCommencing)
            .courtSittings(List.of(sitting1, sitting2))
            .build();
        HearingDto h2 = HearingDto.builder().build();

        String expectedWeekCommencing = "{\"courtHouse\":\"London Court\",\"startDate\":\"2025-01-06\",\"endDate\":\"2025-01-12\",\"durationInWeeks\":1}";
        String expectedSitting1 = "{\"sittingStart\":\"2025-01-01T09:00\",\"sittingEnd\":\"2025-01-01T17:00\",\"judiciaryId\":\"J001\",\"courtHouse\":\"London Court\",\"courtRoom\":\"Room 1\"}";
        String expectedSitting2 = "{\"sittingStart\":\"2025-01-02T09:00\",\"sittingEnd\":\"2025-01-02T17:00\",\"judiciaryId\":\"J002\",\"courtHouse\":\"London Court\",\"courtRoom\":\"Room 2\"}";
        String expected = "{\"hearingId\":\"H001\",\"hearingType\":\"Trial\",\"hearingDescription\":\"Main hearing\",\"listNote\":\"List note\","
            + "\"weekCommencing\":" + expectedWeekCommencing + ",\"courtSittings\":[" + expectedSitting1 + "," + expectedSitting2 + "]}";
        assertEquals(expected, objectMapper.writeValueAsString(h1));
        assertEquals("{\"hearingId\":null,\"hearingType\":null,\"hearingDescription\":null,\"listNote\":null,\"weekCommencing\":null,\"courtSittings\":null}", objectMapper.writeValueAsString(h2));
    }

    @Test
    void testBuilderAndEquals() {
        WeekCommencingDto weekCommencing = WeekCommencingDto.builder()
            .courtHouse("London Court")
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .build();
        CourtSittingDto sitting1 = CourtSittingDto.builder()
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .judiciaryId("J001")
            .courtHouse("London Court")
            .courtRoom("Room 1")
            .build();
        CourtSittingDto sitting2 = CourtSittingDto.builder()
            .sittingStart("2025-01-02T09:00")
            .sittingEnd("2025-01-02T17:00")
            .judiciaryId("J002")
            .courtHouse("London Court")
            .courtRoom("Room 2")
            .build();

        HearingDto h1 = HearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main hearing")
            .listNote("List note")
            .weekCommencing(weekCommencing)
            .courtSittings(List.of(sitting1, sitting2))
            .build();
        HearingDto h2 = HearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main hearing")
            .listNote("List note")
            .weekCommencing(weekCommencing)
            .courtSittings(List.of(sitting1, sitting2))
            .build();

        assertEquals(h1.getHearingId(), h2.getHearingId());
        assertEquals(h1.getHearingType(), h2.getHearingType());
        assertEquals(h1.getHearingDescription(), h2.getHearingDescription());
        assertEquals(h1.getListNote(), h2.getListNote());
        assertEquals(h1.getWeekCommencing(), h2.getWeekCommencing());
        assertEquals(h1.getCourtSittings(), h2.getCourtSittings());

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
}
