package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CaseDetailsService {

    @Autowired
    private CourtScheduleService courtScheduleService;
    @Autowired
    private CourtHouseService courtHouseService;

    @Autowired
    private OAuthTokenService oauthTokenService;

    public CaseDetailsDto getCaseDetailsByCaseUrn(String caseUrn) {
        String token = oauthTokenService.getJwtToken();
        List<CourtScheduleDto> courtSchedule = courtScheduleService.getCourtScheduleByCaseUrn(token, caseUrn);

        CaseDetailsDto caseDetails = new CaseDetailsDto(
            caseUrn,
            courtSchedule.stream()
                .map(schedule -> new CaseDetailsDto.CaseDetailsCourtScheduleDto(
                    schedule.hearingDtos().stream()
                        .map(t -> getHearingDetails(token,t))
                        .filter(Objects::nonNull)
                        .toList()
                ))
                .toList()
        );
        String courtHouseAndRoomIds = caseDetails.courtSchedule().stream()
            .flatMap(a -> a.hearings().stream()
                .flatMap(b -> b.courtSittings().stream()
                    .map(c -> c.courtHouse().courtHouseId() + ":" + c.courtHouse().courtRoomId())))
            .collect(Collectors.joining("  "));

        log.atInfo().log("caseUrn : {} -> Received CourtHouse Id and courtRoomId :{}",
                         caseUrn, courtHouseAndRoomIds);
        return caseDetails;

    }

    private CaseDetailsHearingDto getHearingDetails(String token, CourtScheduleDto.HearingDto hearing) {
        List<CaseDetailsCourtSittingDto> futureSittings = hearing.courtSittingDtos().stream()
            .filter(sitting -> validateSittingDateNotInPast(sitting.sittingStart()))
            .map(a -> getHearingSchedule(token, a))
            .toList();

        if (futureSittings.isEmpty()) {
            return null;
        }

        return new CaseDetailsHearingDto(
            futureSittings,
            hearing.hearingId(),
            hearing.hearingType(),
            hearing.hearingDescription(),
            hearing.listNote()
        );
    }


    private CaseDetailsCourtSittingDto getHearingSchedule(
        String token, CourtScheduleDto.HearingDto.CourtSittingDto sitting) {
        return new CaseDetailsCourtSittingDto(
            sitting.judiciaryId(),
            sitting.sittingStart(),
            sitting.sittingEnd(),
            courtHouseService.getCourtHouseById(token, sitting.courtHouse(), sitting.courtRoom())
        );
    }

    private boolean validateSittingDateNotInPast(String dateTimeString) {
        LocalDate sittingDate = LocalDateTime.parse(dateTimeString).toLocalDate();
        return !sittingDate.isBefore(LocalDate.now());
    }
}

