package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CaseDetailsCourtSittingDtoTest {

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

        CaseDetailsCourtSittingDto s1 = CaseDetailsCourtSittingDto.builder()
            .judiciaryId("J001")
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .courtHouse(courtHouse)
            .build();
        CaseDetailsCourtSittingDto s2 = CaseDetailsCourtSittingDto.builder().build();

        String expectedCourtHouse = """
            {"courtHouseId":"CH001","courtRoomId":"CR001","courtHouseType":"Crown","courtHouseCode":"LON",
            "courtHouseName":"London Court","address":{"address1":"1 Court Street","address2":"Westminster",
            "address3":"London","address4":"Greater London","postalCode":"SW1A 1AA","country":"UK"},
            "courtRoom":[{"courtRoomId":1,"courtRoomName":"Room 1"},{"courtRoomId":2,"courtRoomName":"Room 2"}]}
            """;
        String expected = "{\"judiciaryId\":\"J001\",\"sittingStart\":\"2025-01-01T09:00\","
            + "\"sittingEnd\":\"2025-01-01T17:00\",\"courtHouse\":" + expectedCourtHouse + "}";
        assertEquals(
            objectMapper.readTree(expected).toString(),
            objectMapper.writeValueAsString(s1)
        );
        assertEquals("{}", objectMapper.writeValueAsString(s2));
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

        CaseDetailsCourtSittingDto s1 = CaseDetailsCourtSittingDto.builder()
            .judiciaryId("J001")
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .courtHouse(courtHouse)
            .build();
        CaseDetailsCourtSittingDto s2 = CaseDetailsCourtSittingDto.builder()
            .judiciaryId("J001")
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .courtHouse(courtHouse)
            .build();

        assertEquals(s1.getJudiciaryId(), s2.getJudiciaryId());
        assertEquals(s1.getSittingStart(), s2.getSittingStart());
        assertEquals(s1.getSittingEnd(), s2.getSittingEnd());
        assertEquals(s1.getCourtHouse(), s2.getCourtHouse());

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s1.hashCode());
    }
}
