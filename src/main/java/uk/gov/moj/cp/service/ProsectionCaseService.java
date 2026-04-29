package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.ProsecutionCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.api.ProsecutionCaseAPIClient;
import uk.gov.moj.cp.dto.outbound.ProsecutionCaseDTO;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProsectionCaseService {

    private final ProsecutionCaseAPIClient prosecutionCaseAPIClient;

    public ProsecutionCaseDTO getCaseStatus(final String accessToken, final String caseUrn) {
        HttpEntity<ProsecutionCase> result = prosecutionCaseAPIClient.getCaseDetails(accessToken, caseUrn);

        if (isNull(result) || isNull(result.getBody())) {
            log.atError().log("Response body is null or empty");
            return null;
        }
        return convertToCaseStatusDto(result.getBody());
    }

    private ProsecutionCaseDTO convertToCaseStatusDto(final ProsecutionCase prosecutionCase) {
        return ProsecutionCaseDTO.builder()
            .caseStatus(prosecutionCase.getCaseStatus())
            .reportingRestrictions(prosecutionCase.isReportingRestrictions())
            .build();
    }
}

