package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtScheduleSchema;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;
import com.moj.generated.hmcts.WeekCommencing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.api.CourtScheduleClient;
import uk.gov.moj.cp.dto.inbound.CourtScheduleDto;
import uk.gov.moj.cp.dto.inbound.CourtSittingDto;
import uk.gov.moj.cp.dto.inbound.HearingDto;
import uk.gov.moj.cp.dto.inbound.WeekCommencingDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@Slf4j
public class CourtScheduleService {

    @Autowired
    private CourtScheduleClient courtScheduleClient;

    public List<CourtScheduleDto> getCourtScheduleByCaseUrn(String accessToken, String caseUrn) {
        ResponseEntity<CourtScheduleSchema> result = courtScheduleClient.getCourtScheduleByCaseUrn(
            accessToken,
            caseUrn
        );
        if (result == null || result.getBody() == null) {
            throw new RuntimeException("Response body is null or empty for caseUrn: " + caseUrn);
        }
        return convertToCourtScheduleResult(caseUrn, result.getBody().getCourtSchedule());
    }

    private List<CourtScheduleDto> convertToCourtScheduleResult(String caseUrn,
                                                                List<CourtSchedule> courtScheduleResultList) {
        String hearingIdList = courtScheduleResultList.stream()
            .map(courtSchedule -> courtSchedule.getHearings().stream()
                .map(Hearing::getHearingId)
                .collect(Collectors.joining(",")))
            .collect(Collectors.joining(","));
        log.atInfo().log("Received Hearing Ids : {} for caseUrn : {} ", hearingIdList, caseUrn);

        List<CourtScheduleDto> courtScheduleDtos = courtScheduleResultList.stream()
            .map(courtSchedule ->
                 {
                     List<HearingDto> hearings = courtSchedule.getHearings().stream()
                         .map(this::getHearings)
                         .toList();

                     return CourtScheduleDto.builder()
                         .hearings(hearings)
                         .build();
                 }).toList();
        return courtScheduleDtos;
    }

    private HearingDto getHearings(Hearing hearing) {
        WeekCommencingDto weekCommencingDto = null;
        WeekCommencing weekCommencing = hearing.getWeekCommencing();
        if (weekCommencing != null) {

            weekCommencingDto = WeekCommencingDto.builder()
                .courtHouse(weekCommencing.getCourtHouse())
                .startDate(convertLocalDateToString(weekCommencing.getStartDate()))
                .endDate(convertLocalDateToString(weekCommencing.getEndDate()))
                .durationInWeeks(weekCommencing.getDurationInWeeks())
                .build();
        }

        List<CourtSittingDto> courtSittings = hearing.getCourtSittings().stream().map(this::getCourtSittings).toList();

        return HearingDto.builder()
            .hearingId(hearing.getHearingId())
            .hearingType(hearing.getHearingType())
            .hearingDescription(hearing.getHearingDescription())
            .listNote(hearing.getListNote())
            .weekCommencing(weekCommencingDto)
            .courtSittings(courtSittings)
            .build();
    }

    private String convertLocalDateToString(LocalDate date) {
        return nonNull(date) ? date.format(DateTimeFormatter.ISO_DATE) : null;
    }

    private CourtSittingDto getCourtSittings(CourtSitting courtSitting) {

        return CourtSittingDto.builder()
            .sittingStart(courtSitting.getSittingStart().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .sittingEnd(courtSitting.getSittingEnd().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .judiciaryId(courtSitting.getJudiciaryId())
            .courtHouse(courtSitting.getCourtHouse())
            .courtRoom(courtSitting.getCourtRoom())
            .build();

    }
}

