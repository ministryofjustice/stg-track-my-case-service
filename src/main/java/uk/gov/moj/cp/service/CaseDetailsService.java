package uk.gov.moj.cp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseDetailsDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto;
import uk.gov.moj.cp.dto.CaseDetailsDto.CaseDetailsCourtScheduleDto.CaseDetailsHearingDto.CaseDetailsCourtSittingDto;
import uk.gov.moj.cp.dto.CourtScheduleDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaseDetailsService {

    @Autowired
    private CourtScheduleService courtScheduleService;
    @Autowired
    private CourtHouseService courtHouseService;


    public CaseDetailsDto getCaseDetailsByCaseUrn(String caseUrn) {
        List<CourtScheduleDto> courtSchedule = courtScheduleService.getCourtScheduleByCaseUrn(caseUrn);

        return new CaseDetailsDto(
            courtSchedule.stream()
                .map(schedule -> new CaseDetailsDto.CaseDetailsCourtScheduleDto(
                    schedule.hearingDtos().stream()
                        .map(this::getCaseDetailsHearingDto)
                        .collect(Collectors.toList())
                ))
                .collect(Collectors.toList())
        );
    }

    private CaseDetailsHearingDto getCaseDetailsHearingDto(CourtScheduleDto.HearingDto hearing) {
        return new CaseDetailsHearingDto(
            hearing.courtSittingDtos().stream()
                .map(this::getCaseDetailsCourtSittingDto)
                .collect(Collectors.toList()),
            hearing.hearingId(),
            hearing.hearingType(),
            hearing.hearingDescription(),
            hearing.listNote()
        );
    }

    private CaseDetailsCourtSittingDto getCaseDetailsCourtSittingDto(
        CourtScheduleDto.HearingDto.CourtSittingDto sitting) {
        return new CaseDetailsCourtSittingDto(
            sitting.judiciaryId(),
            sitting.sittingStart(),
            sitting.sittingEnd(),
            courtHouseService.getCourtHouseById(sitting.courtHouse(), sitting.courtRoom())
        );
    }
}

