package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CaseDetailsService {

    @Autowired
    private CourtScheduleService courtScheduleService;
    @Autowired
    private CourtHouseService courtHouseService;


    public CaseDetailsDto getCaseDetailsByCaseUrn(String caseUrn) {
        List<CourtScheduleDto> courtSchedule = courtScheduleService.getCourtScheduleByCaseUrn(caseUrn);

        CaseDetailsDto caseDetails = new CaseDetailsDto(
            caseUrn,
            courtSchedule.stream()
                .map(schedule -> new CaseDetailsDto.CaseDetailsCourtScheduleDto(
                    schedule.hearingDtos().stream()
                        .map(this::getHearingDetails)
                        .collect(Collectors.toList())
                ))
                .collect(Collectors.toList())
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

    private CaseDetailsHearingDto getHearingDetails(CourtScheduleDto.HearingDto hearing) {
        return new CaseDetailsHearingDto(
            hearing.courtSittingDtos().stream()
                .map(this::getHearingSchedule)
                .collect(Collectors.toList()),
            hearing.hearingId(),
            hearing.hearingType(),
            hearing.hearingDescription(),
            hearing.listNote()
        );
    }


    private CaseDetailsCourtSittingDto getHearingSchedule(
        CourtScheduleDto.HearingDto.CourtSittingDto sitting) {
        return new CaseDetailsCourtSittingDto(
            sitting.judiciaryId(),
            sitting.sittingStart(),
            sitting.sittingEnd(),
            courtHouseService.getCourtHouseById(sitting.courtHouse(), sitting.courtRoom())
        );
    }
}

