package uk.gov.moj.cp.dto.outbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class CaseDetailsDtoTest {

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
            .hearingDescription("Main hearing")
            .listNote("List note")
            .courtSittings(List.of(sitting))
            .weekCommencing(weekCommencing)
            .build();
        CaseDetailsCourtScheduleDto courtSchedule = CaseDetailsCourtScheduleDto.builder()
            .hearings(List.of(hearing))
            .build();

        CaseDetailsDto dto1 = CaseDetailsDto.builder()
            .caseUrn("URN123")
            .caseStatus(CaseStatus.ACTIVE)
            .courtSchedules(List.of(courtSchedule))
            .build();
        CaseDetailsDto dto2 = CaseDetailsDto.builder().build();

        String expectedCourtHouse = "{\"courtHouseId\":\"CH001\",\"courtRoomId\":\"CR001\",\"courtHouseType\":\"Crown\",\"courtHouseCode\":\"LON\",\"courtHouseName\":\"London Court\",\"address\":{\"address1\":\"1 Court Street\",\"address2\":\"Westminster\",\"address3\":\"London\",\"address4\":\"Greater London\",\"postalCode\":\"SW1A 1AA\",\"country\":\"UK\"},\"courtRoom\":[{\"courtRoomId\":1,\"courtRoomName\":\"Room 1\"},{\"courtRoomId\":2,\"courtRoomName\":\"Room 2\"}]}";
        String expectedSitting = "{\"judiciaryId\":\"J001\",\"sittingStart\":\"2025-01-01T09:00\",\"sittingEnd\":\"2025-01-01T17:00\",\"courtHouse\":" + expectedCourtHouse + "}";
        String expectedWeekCommencing = "{\"startDate\":\"2025-01-06\",\"endDate\":\"2025-01-12\",\"durationInWeeks\":1,\"courtHouse\":" + expectedCourtHouse + "}";
        String expectedHearing = "{\"hearingId\":\"H001\",\"hearingType\":\"Trial\",\"hearingDescription\":\"Main hearing\",\"listNote\":\"List note\",\"courtSittings\":[" + expectedSitting + "],\"weekCommencing\":" + expectedWeekCommencing + "}";
        String expectedCourtSchedule = "{\"hearings\":[" + expectedHearing + "]}";
        assertEquals(
            "{\"caseUrn\":\"URN123\",\"caseStatus\":\"ACTIVE\",\"courtSchedule\":[" + expectedCourtSchedule + "]}",
            objectMapper.writeValueAsString(dto1)
        );
        assertEquals("{}", objectMapper.writeValueAsString(dto2));
    }

    @Test
    void testJsonInclude_caseStatusOnlyWithUrn() throws JsonProcessingException {
        CaseDetailsDto withStatus = CaseDetailsDto.builder()
            .caseUrn("URN-456")
            .caseStatus("Listed")
            .build();
        assertEquals(
            "{\"caseUrn\":\"URN-456\",\"caseStatus\":\"Listed\"}",
            objectMapper.writeValueAsString(withStatus)
        );

        CaseDetailsDto urnNoStatus = CaseDetailsDto.builder()
            .caseUrn("URN-789")
            .build();
        assertEquals(
            "{\"caseUrn\":\"URN-789\"}",
            objectMapper.writeValueAsString(urnNoStatus)
        );
    }

    @Test
    void testEqualsAndHashCode_includesCaseStatus() {
        CaseDetailsDto a = CaseDetailsDto.builder()
            .caseUrn("URN1")
            .caseStatus(CaseStatus.ACTIVE)
            .courtSchedules(List.of())
            .build();
        CaseDetailsDto b = CaseDetailsDto.builder()
            .caseUrn("URN1")
            .caseStatus(CaseStatus.ACTIVE)
            .courtSchedules(List.of())
            .build();
        CaseDetailsDto differentStatus = CaseDetailsDto.builder()
            .caseUrn("URN1")
            .caseStatus(CaseStatus.INACTIVE)
            .courtSchedules(List.of())
            .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, differentStatus);
    }

    @Test
    void testBuilderAndEquals() {
        CourtRoomDto cr1 = CourtRoomDto.builder()
            .courtRoomId(100)
            .courtRoomName("Court Room Name")
            .build();
        CourtRoomDto cr2 = CourtRoomDto.builder()
            .courtRoomId(100)
            .courtRoomName("Court Room Name").build();

        assertEquals(cr1.getCourtRoomId(), cr2.getCourtRoomId());
        assertEquals(cr1.getCourtRoomName(), cr2.getCourtRoomName());

        assertEquals(cr1, cr2);
        assertEquals(cr1.hashCode(), cr2.hashCode());
    }
}
