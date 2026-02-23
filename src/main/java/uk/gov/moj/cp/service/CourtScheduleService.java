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
import uk.gov.moj.cp.client.CourtScheduleClient;
import uk.gov.moj.cp.dto.CourtScheduleDto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
@Slf4j
public class CourtScheduleService {

    @Autowired
    private CourtScheduleClient courtScheduleClient;

    public List<CourtScheduleDto> getCourtScheduleByCaseUrn(String accessToken, String caseUrn) {
        ResponseEntity<CourtScheduleSchema> result = courtScheduleClient.getCourtScheduleByCaseUrn(accessToken,
                                                                                                   caseUrn);
        if (result == null || result.getBody() == null) {
            throw new RuntimeException("Response body is null or empty for caseUrn: " + caseUrn);
        }
        return convertToCourtScheduleResult(caseUrn, result.getBody().getCourtSchedule());
    }

    private List<CourtScheduleDto> convertToCourtScheduleResult(String caseUrn,
                                                                List<CourtSchedule> courtScheduleResultList) {
        String hearingIdList = courtScheduleResultList.stream().map(a ->
                                                                        a.getHearings().stream().map(
                                                                            b -> b.getHearingId()).collect(
                                                                            Collectors.joining(",")))
                        .collect(Collectors.joining(","));

        log.atInfo().log("Received Hearing Ids : {} for caseUrn : {} ", hearingIdList, caseUrn);
        return courtScheduleResultList.stream().map(a ->
            new CourtScheduleDto(
                a.getHearings().stream().map(this::getHearings).collect(Collectors.toUnmodifiableList())
            )).collect(Collectors.toUnmodifiableList());
    }

    private CourtScheduleDto.HearingDto getHearings(Hearing hearing) {
        CourtScheduleDto.HearingDto.WeekCommencingDto weekCommencingDto = null;
        if(Optional.ofNullable(hearing.getWeekCommencing()).isPresent()) {
            WeekCommencing weekCommencing = Optional.ofNullable(hearing.getWeekCommencing()).isPresent() ? hearing.getWeekCommencing() : null;
            weekCommencingDto = new CourtScheduleDto.HearingDto.WeekCommencingDto(
                weekCommencing.getCourtHouse(),
                convertLocalDateToString(weekCommencing.getStartDate()),
                convertLocalDateToString(weekCommencing.getEndDate()),
                weekCommencing.getDurationInWeeks()
            );
        }
        return new CourtScheduleDto.HearingDto(
            hearing.getHearingId(),
            hearing.getHearingType(),
            hearing.getHearingDescription(),
            hearing.getListNote(),
            weekCommencingDto,
            hearing.getCourtSittings().stream().map(this::getCourtSittings).collect(Collectors.toUnmodifiableList())
        );
    }

    private String convertLocalDateToString(LocalDate date) {
        return nonNull(date)  ? date.format(DateTimeFormatter.ISO_DATE) : null;
    }

    private CourtScheduleDto.HearingDto.CourtSittingDto getCourtSittings(CourtSitting courtSitting) {
        return new CourtScheduleDto.HearingDto.CourtSittingDto(
            courtSitting.getSittingStart().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            courtSitting.getSittingEnd().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            courtSitting.getJudiciaryId(),
            courtSitting.getCourtHouse(),
            courtSitting.getCourtRoom()
        );
    }
}

