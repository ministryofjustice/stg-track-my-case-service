package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CourtHouseDtoTest {

    @Test
    void testJsonInclude() throws JsonProcessingException {
        AddressDto address = AddressDto.builder()
            .address1("1 Court Street")
            .address2("Westminster")
            .address3("London")
            .address4("Greater London")
            .postalCode("SW1A 1AA")
            .country("UK")
            .build();
        CourtRoomDto courtRoom1 = CourtRoomDto.builder()
            .courtRoomId(1)
            .courtRoomName("Room 1")
            .build();
        CourtRoomDto courtRoom2 = CourtRoomDto.builder()
            .courtRoomId(2)
            .courtRoomName("Room 2")
            .build();

        CourtHouseDto ch1 = CourtHouseDto.builder()
            .courtHouseId("CH001")
            .courtRoomId("CR001")
            .courtHouseType("Crown")
            .courtHouseCode("LON")
            .courtHouseName("London Court")
            .address(address)
            .courtRooms(List.of(courtRoom1, courtRoom2))
            .build();
        CourtHouseDto ch2 = CourtHouseDto.builder().build();

        assertEquals(
            "{\"courtHouseId\":\"CH001\",\"courtRoomId\":\"CR001\",\"courtHouseType\":\"Crown\",\"courtHouseCode\":\"LON\",\"courtHouseName\":\"London Court\",\"address\":{\"address1\":\"1 Court Street\",\"address2\":\"Westminster\",\"address3\":\"London\",\"address4\":\"Greater London\",\"postalCode\":\"SW1A 1AA\",\"country\":\"UK\"},\"courtRoom\":[{\"courtRoomId\":1,\"courtRoomName\":\"Room 1\"},{\"courtRoomId\":2,\"courtRoomName\":\"Room 2\"}]}",
            objectMapper.writeValueAsString(ch1)
        );
        assertEquals("{}", objectMapper.writeValueAsString(ch2));
    }

    @Test
    void testBuilderAndEquals() {
        AddressDto address = AddressDto.builder()
            .address1("1 Court Street")
            .address2("Westminster")
            .address3("London")
            .address4("Greater London")
            .postalCode("SW1A 1AA")
            .country("UK")
            .build();
        CourtRoomDto courtRoom1 = CourtRoomDto.builder()
            .courtRoomId(1)
            .courtRoomName("Room 1")
            .build();
        CourtRoomDto courtRoom2 = CourtRoomDto.builder()
            .courtRoomId(2)
            .courtRoomName("Room 2")
            .build();

        CourtHouseDto ch1 = CourtHouseDto.builder()
            .courtHouseId("CH001")
            .courtRoomId("CR001")
            .courtHouseType("Crown")
            .courtHouseCode("LON")
            .courtHouseName("London Court")
            .address(address)
            .courtRooms(List.of(courtRoom1, courtRoom2))
            .build();
        CourtHouseDto ch2 = CourtHouseDto.builder()
            .courtHouseId("CH001")
            .courtRoomId("CR001")
            .courtHouseType("Crown")
            .courtHouseCode("LON")
            .courtHouseName("London Court")
            .address(address)
            .courtRooms(List.of(courtRoom1, courtRoom2))
            .build();

        assertEquals(ch1.getCourtHouseId(), ch2.getCourtHouseId());
        assertEquals(ch1.getCourtRoomId(), ch2.getCourtRoomId());
        assertEquals(ch1.getCourtHouseType(), ch2.getCourtHouseType());
        assertEquals(ch1.getCourtHouseCode(), ch2.getCourtHouseCode());
        assertEquals(ch1.getCourtHouseName(), ch2.getCourtHouseName());
        assertEquals(ch1.getAddress(), ch2.getAddress());
        assertEquals(ch1.getCourtRooms(), ch2.getCourtRooms());

        assertEquals(ch1, ch2);
        assertEquals(ch1.hashCode(), ch2.hashCode());
    }
}
