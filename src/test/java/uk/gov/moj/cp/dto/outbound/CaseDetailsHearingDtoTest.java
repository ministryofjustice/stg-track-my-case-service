package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CaseDetailsHearingDtoTest {

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
        CaseDetailsCourtSittingDto sitting1 = CaseDetailsCourtSittingDto.builder()
            .judiciaryId("J001")
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .courtHouse(courtHouse)
            .build();
        CaseDetailsWeekCommencingDto weekCommencing = CaseDetailsWeekCommencingDto.builder()
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .courtHouse(courtHouse)
            .build();

        CaseDetailsHearingDto h1 = CaseDetailsHearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main hearing")
            .listNote("List note")
            .courtSittings(List.of(sitting1))
            .weekCommencing(weekCommencing)
            .build();
        CaseDetailsHearingDto h2 = CaseDetailsHearingDto.builder().build();

        String expectedCourtHouse = "{\"courtHouseId\":\"CH001\",\"courtRoomId\":\"CR001\",\"courtHouseType\":\"Crown\",\"courtHouseCode\":\"LON\",\"courtHouseName\":\"London Court\",\"address\":{\"address1\":\"1 Court Street\",\"address2\":\"Westminster\",\"address3\":\"London\",\"address4\":\"Greater London\",\"postalCode\":\"SW1A 1AA\",\"country\":\"UK\"},\"courtRoom\":[{\"courtRoomId\":1,\"courtRoomName\":\"Room 1\"},{\"courtRoomId\":2,\"courtRoomName\":\"Room 2\"}]}";
        String expectedSitting = "{\"judiciaryId\":\"J001\",\"sittingStart\":\"2025-01-01T09:00\",\"sittingEnd\":\"2025-01-01T17:00\",\"courtHouse\":" + expectedCourtHouse + "}";
        String expectedWeekCommencing = "{\"startDate\":\"2025-01-06\",\"endDate\":\"2025-01-12\",\"durationInWeeks\":1,\"courtHouse\":" + expectedCourtHouse + "}";
        assertEquals(
            "{\"hearingId\":\"H001\",\"hearingType\":\"Trial\",\"hearingDescription\":\"Main hearing\",\"listNote\":\"List note\",\"courtSittings\":[" + expectedSitting + "],\"weekCommencing\":" + expectedWeekCommencing + "}",
            objectMapper.writeValueAsString(h1)
        );
        assertEquals("{}", objectMapper.writeValueAsString(h2));
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
        CaseDetailsCourtSittingDto sitting1 = CaseDetailsCourtSittingDto.builder()
            .judiciaryId("J001")
            .sittingStart("2025-01-01T09:00")
            .sittingEnd("2025-01-01T17:00")
            .courtHouse(courtHouse)
            .build();
        CaseDetailsCourtSittingDto sitting2 = CaseDetailsCourtSittingDto.builder()
            .judiciaryId("J002")
            .sittingStart("2025-01-02T09:00")
            .sittingEnd("2025-01-02T17:00")
            .courtHouse(courtHouse)
            .build();
        CaseDetailsWeekCommencingDto weekCommencing = CaseDetailsWeekCommencingDto.builder()
            .startDate("2025-01-06")
            .endDate("2025-01-12")
            .durationInWeeks(1)
            .courtHouse(courtHouse)
            .build();

        CaseDetailsHearingDto h1 = CaseDetailsHearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main hearing")
            .listNote("List note")
            .courtSittings(List.of(sitting1, sitting2))
            .weekCommencing(weekCommencing)
            .build();
        CaseDetailsHearingDto h2 = CaseDetailsHearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main hearing")
            .listNote("List note")
            .courtSittings(List.of(sitting1, sitting2))
            .weekCommencing(weekCommencing)
            .build();

        assertEquals(h1.getHearingId(), h2.getHearingId());
        assertEquals(h1.getHearingType(), h2.getHearingType());
        assertEquals(h1.getHearingDescription(), h2.getHearingDescription());
        assertEquals(h1.getListNote(), h2.getListNote());
        assertEquals(h1.getCourtSittings(), h2.getCourtSittings());
        assertEquals(h1.getWeekCommencing(), h2.getWeekCommencing());

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
}
