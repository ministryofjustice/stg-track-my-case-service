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
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CourtHouseDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto;
import uk.gov.moj.cp.dto.CourtHouseDto.CourtRoomDto.AddressDto;
import uk.gov.moj.cp.service.CaseDetailsService;

import java.time.LocalDateTime;
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
        final CourtHouseDto courtHouseDto = new CourtHouseDto(
            courtHouseId,
            courtRoomId,
            "CROWN",
            "CH123",
            "Lavender Hill",
            new AddressDto(
                "123 ",
                "Court Street",
                "London",
                null,
                "TE1 1ST",
                "UK"
            ),
            List.of(new CourtRoomDto(1, "CourtRoom 01"))
        );

        final CaseDetailsCourtSittingDto courtSitting = new CaseDetailsCourtSittingDto(
                judiciaryId,
                sittingStartDate,
                sittingEndDate,
                courtHouseDto
            );

        final CaseDetailsHearingDto hearing = new CaseDetailsHearingDto(
                List.of(courtSitting),
                hearingId,
                "First Hearing",
                "Initial hearing description",
                "Test note"
            );

        final List<CaseDetailsCourtScheduleDto> courtSchedules = List.of(
            new CaseDetailsCourtScheduleDto(List.of(hearing)));

        final CaseDetailsDto caseDetailsDto = new CaseDetailsDto(caseUrn, courtSchedules);

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

        CaseDetailsDto caseDetailsDto = new CaseDetailsDto(caseUrn, new ArrayList<>());

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
                                "An error occurred while processing the request either caseUrn is not available. "
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
                "An error occurred while processing the request either caseUrn is not available. "
                    + "or see the logs for more details."
            ));
    }

}
