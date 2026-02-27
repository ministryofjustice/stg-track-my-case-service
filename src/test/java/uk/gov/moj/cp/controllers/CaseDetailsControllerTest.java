package uk.gov.moj.cp.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.moj.cp.dto.outbound.CaseDetailsDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsCourtScheduleDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.outbound.CourtHouseDto;
import uk.gov.moj.cp.dto.outbound.CourtRoomDto;
import uk.gov.moj.cp.dto.outbound.AddressDto;
import uk.gov.moj.cp.dto.outbound.CaseDetailsWeekCommencingDto;
import uk.gov.moj.cp.service.CaseDetailsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CaseDetailsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CaseDetailsService caseDetailsService;

    @InjectMocks
    private CaseDetailsController caseDetailsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(caseDetailsController).build();
    }

    @Test
    @DisplayName("GET /api/cases/{case_urn}/casedetails - success with populated hearing schedule")
    void shouldGetCaseDetailsByCaseUrnWithPopulatedHearingSchedule() throws Exception {
        final String caseUrn = "CASE123";
        final String hearingId = UUID.randomUUID().toString();
        final String courtHouseId = UUID.randomUUID().toString();
        final String courtRoomId = UUID.randomUUID().toString();
        final String judiciaryId = UUID.randomUUID().toString();
        final String sittingStartDate = LocalDateTime.now().toString();
        final String sittingEndDate = LocalDateTime.now().plusHours(2).toString();

        // Create test data using correct DTO structure

        final CourtHouseDto courtHouseDto = CourtHouseDto.builder()
            .courtHouseId(courtHouseId)
            .courtRoomId(courtRoomId)
            .courtHouseType("CROWN")
            .courtHouseCode("CH123")
            .courtHouseName("Lavender Hill")
            .address(AddressDto.builder()
                         .address1("123")
                         .address2("Court Street")
                         .address3("London")
                         .postalCode("TE1 1ST")
                         .country("UK")
                         .build())
            .courtRooms( List.of(CourtRoomDto.builder()
                                    .courtRoomId(123)
                                    .courtRoomName("CourtRoom 01")
                                    .build()))
            .build();


        final CaseDetailsCourtSittingDto courtSitting = CaseDetailsCourtSittingDto.builder()
            .judiciaryId(judiciaryId)
            .sittingStart(sittingStartDate)
            .sittingEnd(sittingEndDate)
            .courtHouse(courtHouseDto)
            .build();

        final CaseDetailsHearingDto hearing = CaseDetailsHearingDto.builder()
            .hearingId(hearingId)
            .hearingType("First Hearing")
            .hearingDescription("Initial hearing description")
            .listNote("Test note")
            .courtSittings(List.of(courtSitting))
            .weekCommencing(CaseDetailsWeekCommencingDto.builder()
                                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                                .endDate(LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_DATE))
                                .durationInWeeks(2)
                                .build())
            .build();

        final List<CaseDetailsCourtScheduleDto> courtSchedules = List.of(
            CaseDetailsCourtScheduleDto.builder()
                .hearings(List.of(hearing))
                .build());

        final CaseDetailsDto caseDetailsDto = CaseDetailsDto.builder()
            .caseUrn(caseUrn)
            .courtSchedules(courtSchedules)
            .build();

        when(caseDetailsService.getCaseDetailsByCaseUrn(caseUrn)).thenReturn(caseDetailsDto);

        mockMvc.perform(get("/api/cases/{case_urn}/casedetails", caseUrn)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caseUrn").value(caseUrn))
            .andExpect(jsonPath("$.courtSchedule").isArray())
            .andExpect(jsonPath("$.courtSchedule.length()").value(1))
            .andExpect(jsonPath("$.courtSchedule[0].hearings").isArray())
            .andExpect(jsonPath("$.courtSchedule[0].hearings.length()").value(1))
            .andExpect(jsonPath("$.courtSchedule[0].hearings[0].hearingId").value(hearingId))
            .andExpect(jsonPath("$.courtSchedule[0].hearings[0].hearingType").value("First Hearing"))
            .andExpect(jsonPath("$.courtSchedule[0].hearings[0].courtSittings[0].sittingStart")
                           .value(sittingStartDate))
            .andExpect(jsonPath("$.courtSchedule[0].hearings[0].courtSittings[0].courtHouse.courtHouseId")
                           .value(courtHouseId))
            .andExpect(jsonPath("$.courtSchedule[0].hearings[0].courtSittings[0].courtHouse.address.address2")
                           .value("Court Street"))
            .andExpect(jsonPath("$.courtSchedule[0].hearings[0].courtSittings[0].courtHouse.courtRoom[0].courtRoomName")
                           .value("CourtRoom 01"));
    }


    @Test
    @DisplayName("GET /api/cases/{case_urn}/casedetails - success with empty court schedule")
    void shouldHandleGetCaseDetailsByCaseUrnWithEmptyHearing() throws Exception {
        String caseUrn = "CASE123";

        final CaseDetailsDto caseDetailsDto = CaseDetailsDto.builder()
            .caseUrn(caseUrn)
            .courtSchedules(new ArrayList<>())
            .build();

        when(caseDetailsService.getCaseDetailsByCaseUrn(caseUrn)).thenReturn(caseDetailsDto);

        mockMvc.perform(get("/api/cases/{case_urn}/casedetails", caseUrn)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caseUrn").value(caseUrn))
            .andExpect(jsonPath("$.courtSchedule").isArray())
            .andExpect(jsonPath("$.courtSchedule.length()").value(0));
    }


    @Test
    @DisplayName("GET /api/cases/{case_urn}/casedetails - service throws IllegalArgumentException")
    void shouldHandleIllegalArgumentException() throws Exception {
        String caseUrn = "INVALID_FORMAT";

        when(caseDetailsService.getCaseDetailsByCaseUrn(caseUrn))
            .thenThrow(new IllegalArgumentException("Invalid case URN format"));

        mockMvc.perform(get("/api/cases/{case_urn}/casedetails", caseUrn)
                            .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isNotFound())
                            .andExpect(content().string(
                                "An error occurred while processing the request either caseUrn is not available,"
                                    + "or see the logs for more details."
                            ));
    }

    @Test
    @DisplayName("GET /api/cases/{case_urn}/casedetails - service throws NullPointerException")
    void shouldHandleNullPointerException() throws Exception {
        String caseUrn = "NULL_CASE";

        when(caseDetailsService.getCaseDetailsByCaseUrn(caseUrn))
            .thenThrow(new NullPointerException("Null pointer in service"));

        mockMvc.perform(get("/api/cases/{case_urn}/casedetails", caseUrn)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content().string(
                "An error occurred while processing the request either caseUrn is not available,"
                    + "or see the logs for more details."
            ));
    }

}
