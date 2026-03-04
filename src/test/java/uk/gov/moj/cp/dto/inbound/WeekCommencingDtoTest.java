package uk.gov.moj.cp.dto.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class WeekCommencingDtoTest {

    @Test
    void testJsonInclude() throws JsonProcessingException {
        WeekCommencingDto w1 = WeekCommencingDto.builder()
            .courtHouse("London Court")
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .build();
        WeekCommencingDto w2 = WeekCommencingDto.builder().build();

        assertEquals(
            "{\"courtHouse\":\"London Court\",\"startDate\":\"2025-01-06\",\"endDate\":\"2025-01-12\",\"durationInWeeks\":1}",
            objectMapper.writeValueAsString(w1)
        );
        assertEquals("{\"courtHouse\":null,\"startDate\":null,\"endDate\":null,\"durationInWeeks\":0}", objectMapper.writeValueAsString(w2));
    }

    @Test
    void testBuilderAndEquals() {
        WeekCommencingDto w1 = WeekCommencingDto.builder()
            .courtHouse("London Court")
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .build();
        WeekCommencingDto w2 = WeekCommencingDto.builder()
            .courtHouse("London Court")
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .build();

        assertEquals(w1.getCourtHouse(), w2.getCourtHouse());
        assertEquals(w1.getStartDate(), w2.getStartDate());
        assertEquals(w1.getEndDate(), w2.getEndDate());
        assertEquals(w1.getDurationInWeeks(), w2.getDurationInWeeks());

        assertEquals(w1, w2);
        assertEquals(w1.hashCode(), w2.hashCode());
    }
}
