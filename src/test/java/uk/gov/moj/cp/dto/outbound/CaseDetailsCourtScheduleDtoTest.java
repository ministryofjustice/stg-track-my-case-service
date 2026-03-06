package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CaseDetailsCourtScheduleDtoTest {

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
        CaseDetailsCourtSittingDto sitting = CaseDetailsCourtSittingDto.builder()
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
        CaseDetailsHearingDto hearing = CaseDetailsHearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main trial hearing")
            .listNote("List note one")
            .courtSittings(List.of(sitting))
            .weekCommencing(weekCommencing)
            .build();

        CaseDetailsCourtScheduleDto cs1 = CaseDetailsCourtScheduleDto.builder()
            .hearings(List.of(hearing))
            .build();
        CaseDetailsCourtScheduleDto cs2 = CaseDetailsCourtScheduleDto.builder().build();

        String expectedCourtHouse = "{\"courtHouseId\":\"CH001\",\"courtRoomId\":\"CR001\",\"courtHouseType\":\"Crown\",\"courtHouseCode\":\"LON\",\"courtHouseName\":\"London Court\",\"address\":{\"address1\":\"1 Court Street\",\"address2\":\"Westminster\",\"address3\":\"London\",\"address4\":\"Greater London\",\"postalCode\":\"SW1A 1AA\",\"country\":\"UK\"},\"courtRoom\":[{\"courtRoomId\":1,\"courtRoomName\":\"Room 1\"},{\"courtRoomId\":2,\"courtRoomName\":\"Room 2\"}]}";
        String expectedSitting = "{\"judiciaryId\":\"J001\",\"sittingStart\":\"2025-01-01T09:00\",\"sittingEnd\":\"2025-01-01T17:00\",\"courtHouse\":" + expectedCourtHouse + "}";
        String expectedWeekCommencing = "{\"startDate\":\"2025-01-06\",\"endDate\":\"2025-01-12\",\"durationInWeeks\":1,\"courtHouse\":" + expectedCourtHouse + "}";
        String expectedHearing = "{\"hearingId\":\"H001\",\"hearingType\":\"Trial\",\"hearingDescription\":\"Main trial hearing\",\"listNote\":\"List note one\",\"courtSittings\":[" + expectedSitting + "],\"weekCommencing\":" + expectedWeekCommencing + "}";
        assertEquals(
            "{\"hearings\":[" + expectedHearing + "]}",
            objectMapper.writeValueAsString(cs1)
        );
        assertEquals("{}", objectMapper.writeValueAsString(cs2));
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
        CaseDetailsHearingDto hearing1 = CaseDetailsHearingDto.builder()
            .hearingId("H001")
            .hearingType("Trial")
            .hearingDescription("Main trial hearing")
            .listNote("List note one")
            .courtSittings(List.of(sitting1, sitting2))
            .weekCommencing(weekCommencing)
            .build();
        CaseDetailsWeekCommencingDto weekCommencing2 = CaseDetailsWeekCommencingDto.builder()
            .startDate("2025-01-13")
            .endDate("2025-01-19")
            .durationInWeeks(2)
            .courtHouse(courtHouse)
            .build();
        CaseDetailsHearingDto hearing2 = CaseDetailsHearingDto.builder()
            .hearingId("H002")
            .hearingType("Preliminary")
            .hearingDescription("Preliminary hearing")
            .listNote("List note two")
            .courtSittings(List.of(sitting1))
            .weekCommencing(weekCommencing2)
            .build();

        CaseDetailsCourtScheduleDto cs1 = CaseDetailsCourtScheduleDto.builder()
            .hearings(List.of(hearing1, hearing2))
            .build();
        CaseDetailsCourtScheduleDto cs2 = CaseDetailsCourtScheduleDto.builder()
            .hearings(List.of(hearing1, hearing2))
            .build();

        assertEquals(cs1.getHearings(), cs2.getHearings());

        assertEquals(cs1, cs2);
        assertEquals(cs1.hashCode(), cs2.hashCode());
    }
}
