package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CaseDetailsWeekCommencingDtoTest {

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
        CourtHouseDto courtHouse = CourtHouseDto.builder()
            .courtHouseId("CH001")
            .courtRoomId("CR001")
            .courtHouseType("Crown")
            .courtHouseCode("LON")
            .courtHouseName("London Court")
            .address(address)
            .courtRooms(List.of(courtRoom1, courtRoom2))
            .build();

        CaseDetailsWeekCommencingDto w1 = CaseDetailsWeekCommencingDto.builder()
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .courtHouse(courtHouse)
            .build();
        CaseDetailsWeekCommencingDto w2 = CaseDetailsWeekCommencingDto.builder().build();

        String expectedCourtHouse = """
            {"courtHouseId":"CH001","courtRoomId":"CR001","courtHouseType":"Crown","courtHouseCode":"LON",
            "courtHouseName":"London Court","address":{"address1":"1 Court Street","address2":"Westminster",
            "address3":"London","address4":"Greater London","postalCode":"SW1A 1AA","country":"UK"},
            "courtRoom":[{"courtRoomId":1,"courtRoomName":"Room 1"},{"courtRoomId":2,"courtRoomName":"Room 2"}]}
            """;
        String expected = "{\"startDate\":\"2025-01-06\",\"endDate\":\"2025-01-12\",\"durationInWeeks\":1,"
            + "\"courtHouse\":" + expectedCourtHouse + "}";
        assertEquals(objectMapper.readTree(expected).toString(), objectMapper.writeValueAsString(w1));
        assertEquals("{\"durationInWeeks\":0}", objectMapper.writeValueAsString(w2));
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
        CourtHouseDto courtHouse = CourtHouseDto.builder()
            .courtHouseId("CH001")
            .courtRoomId("CR001")
            .courtHouseType("Crown")
            .courtHouseCode("LON")
            .courtHouseName("London Court")
            .address(address)
            .courtRooms(List.of(courtRoom1, courtRoom2))
            .build();

        CaseDetailsWeekCommencingDto w1 = CaseDetailsWeekCommencingDto.builder()
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .courtHouse(courtHouse)
            .build();
        CaseDetailsWeekCommencingDto w2 = CaseDetailsWeekCommencingDto.builder()
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .courtHouse(courtHouse)
            .build();

        assertEquals(w1.getStartDate(), w2.getStartDate());
        assertEquals(w1.getEndDate(), w2.getEndDate());
        assertEquals(w1.getDurationInWeeks(), w2.getDurationInWeeks());
        assertEquals(w1.getCourtHouse(), w2.getCourtHouse());

        assertEquals(w1, w2);
        assertEquals(w1.hashCode(), w2.hashCode());
    }
}
