package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtScheduleSchema;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.CourtScheduleClient;
import uk.gov.moj.cp.dto.CourtScheduleDto;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourtScheduleService {

    @Autowired
    private CourtScheduleClient courtScheduleClient;

    public List<CourtScheduleDto> getCourtScheduleByCaseUrn(String caseUrn) {
        ResponseEntity<CourtScheduleSchema> result = courtScheduleClient.getCourtScheduleByCaseUrn(caseUrn);
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
        return new CourtScheduleDto.HearingDto(
            hearing.getHearingId(),
            hearing.getHearingType(),
            hearing.getHearingDescription(),
            hearing.getListNote(),
            hearing.getCourtSittings().stream().map(this::getCourtSittings).collect(Collectors.toUnmodifiableList())
        );
    }

    private CourtScheduleDto.HearingDto.CourtSittingDto getCourtSittings(CourtSitting courtSitting) {
        return new CourtScheduleDto.HearingDto.CourtSittingDto(
            getBSTDateAndTime(courtSitting.getSittingStart()),
            getBSTDateAndTime(courtSitting.getSittingEnd()),
            courtSitting.getJudiciaryId(),
            courtSitting.getCourtHouse(),
            courtSitting.getCourtRoom()
        );
    }

    private String getBSTDateAndTime(Date date) {
        return Optional.ofNullable(date)
            .map(d -> d.toInstant().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Europe/London"))
                .toLocalDateTime().toString())
            .orElse("N/A");
    }

}

