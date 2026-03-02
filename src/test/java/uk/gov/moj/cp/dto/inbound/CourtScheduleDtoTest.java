package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CourtScheduleDtoTest {

    @Test
    void testJsonInclude() throws JsonProcessingException {
        WeekCommencingDto weekCommencing = WeekCommencingDto.builder()
            .courtHouse("London Court")
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .build();
        CourtSittingDto sitting = CourtSittingDto.builder()
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .judiciaryId("J001")
            .courtHouse("London Court")
            .courtRoom("Room 1")
            .build();
        HearingDto hearing = HearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main trial hearing")
            .listNote("List note one")
            .weekCommencing(weekCommencing)
            .courtSittings(List.of(sitting))
            .build();

        CourtScheduleDto cs1 = CourtScheduleDto.builder()
            .hearings(List.of(hearing))
            .build();
        CourtScheduleDto cs2 = CourtScheduleDto.builder().build();

        String expectedWeekCommencing = "{\"courtHouse\":\"London Court\",\"startDate\":\"2025-01-06\",\"endDate\":\"2025-01-12\",\"durationInWeeks\":1}";
        String expectedSitting = "{\"sittingStart\":\"2025-01-01T09:00\",\"sittingEnd\":\"2025-01-01T17:00\",\"judiciaryId\":\"J001\",\"courtHouse\":\"London Court\",\"courtRoom\":\"Room 1\"}";
        String expectedHearing = "{\"hearingId\":\"H001\",\"hearingType\":\"Trial\",\"hearingDescription\":\"Main trial hearing\",\"listNote\":\"List note one\",\"weekCommencing\":" + expectedWeekCommencing + ",\"courtSittings\":[" + expectedSitting + "]}";
        assertEquals(
            "{\"hearings\":[" + expectedHearing + "]}",
            objectMapper.writeValueAsString(cs1)
        );
        assertEquals("{\"hearings\":null}", objectMapper.writeValueAsString(cs2));
    }

    @Test
    void testBuilderAndEquals() {
        WeekCommencingDto weekCommencing = WeekCommencingDto.builder()
            .courtHouse("London Court")
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .build();
        CourtSittingDto sitting = CourtSittingDto.builder()
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .judiciaryId("J001")
            .courtHouse("London Court")
            .courtRoom("Room 1")
            .build();
        HearingDto hearing = HearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main trial hearing")
            .listNote("List note one")
            .weekCommencing(weekCommencing)
            .courtSittings(List.of(sitting))
            .build();

        CourtScheduleDto cs1 = CourtScheduleDto.builder()
            .hearings(List.of(hearing))
            .build();
        CourtScheduleDto cs2 = CourtScheduleDto.builder()
            .hearings(List.of(hearing))
            .build();

        assertEquals(cs1.getHearings(), cs2.getHearings());

        assertEquals(cs1, cs2);
        assertEquals(cs1.hashCode(), cs2.hashCode());
    }
}
