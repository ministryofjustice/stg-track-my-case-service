package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.ProsecutionCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.api.ProsecutionCaseClient;
import uk.gov.moj.cp.dto.outbound.CaseStatusDto;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProsectionCaseService {

    private final ProsecutionCaseClient prosecutionCaseClient;

    public CaseStatusDto getCaseStatus(String accessToken, String caseUrn) {
        HttpEntity<ProsecutionCase> result = prosecutionCaseClient.getCaseStatus(accessToken, caseUrn);

        if (isNull(result) || isNull(result.getBody())) {
            log.atError().log("Response body is null or empty");
            return null;
        }
        return convertToCaseStatusDto(result.getBody());
    }

    private CaseStatusDto convertToCaseStatusDto(ProsecutionCase prosecutionCase) {
        return CaseStatusDto.builder()
            .caseStatus(prosecutionCase.getCaseStatus())
            .reportingRestrictions(prosecutionCase.getReportingRestrictions())
            .build();
    }
}

