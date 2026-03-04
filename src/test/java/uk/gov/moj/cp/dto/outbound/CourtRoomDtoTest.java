package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CourtRoomDtoTest {

    @Test
    void testJsonInclude() throws JsonProcessingException {
        CourtRoomDto cr1 = CourtRoomDto.builder()
            .courtRoomId(100)
            .courtRoomName("Court Room Name")
            .build();
        CourtRoomDto cr2 = CourtRoomDto.builder().build();

        assertEquals(
            "{\"courtRoomId\":100,\"courtRoomName\":\"Court Room Name\"}",
            objectMapper.writeValueAsString(cr1)
        );
        assertEquals("{\"courtRoomId\":0}", objectMapper.writeValueAsString(cr2));
    }

    @Test
    void testBuilderAndEquals() {
        CourtRoomDto cr1 = CourtRoomDto.builder()
            .courtRoomId(100)
            .courtRoomName("Court Room Name")
            .build();
        CourtRoomDto cr2 = CourtRoomDto.builder()
            .courtRoomId(100)
            .courtRoomName("Court Room Name")
            .build();

        assertEquals(cr1.getCourtRoomId(), cr2.getCourtRoomId());
        assertEquals(cr1.getCourtRoomName(), cr2.getCourtRoomName());

        assertEquals(cr1, cr2);
        assertEquals(cr1.hashCode(), cr2.hashCode());
    }
}
