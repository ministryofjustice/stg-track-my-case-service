package uk.gov.moj.cp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.CourtScheduleClient;
import uk.gov.moj.cp.dto.CourtScheduleDto;
import uk.gov.moj.cp.util.Utils;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.moj.cp.util.Utils.getJsonNode;

@Service
public class CourtScheduleService {

    @Autowired
    private CourtScheduleClient courtScheduleClient;

    public List<CourtScheduleDto> getCourtScheduleByCaseUrn(String caseUrn) {
        HttpEntity<String> result = courtScheduleClient.getCourtScheduleByCaseUrn(caseUrn);

        if (result == null || result.getBody() == null || result.getBody().isEmpty()) {
            throw new RuntimeException("Response body is null or empty");
        }

        try {
            JsonNode courtSchedule = getJsonNode(result.getBody(), "courtSchedule");
            List<CourtSchedule> courtScheduleResultList = Utils.convertJsonStringToList(
                courtSchedule.toString(),
                CourtSchedule.class
            );
            JSONObject aa = new JSONObject().put("aa", courtScheduleResultList);
            return convertToCourtScheduleResult(courtScheduleResultList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<CourtScheduleDto> convertToCourtScheduleResult(List<CourtSchedule> courtScheduleResultList) {
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
            courtSitting.getSittingStart().toString(),
            courtSitting.getSittingEnd().toString(),
            courtSitting.getJudiciaryId(),
            courtSitting.getCourtHouse()
        );
    }
}

