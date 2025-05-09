package uk.gov.moj.cp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.moj.generated.hmcts.CaseresultsSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.CrimeCaseClient;
import uk.gov.moj.cp.dto.CaseJudiciaryResult;
import uk.gov.moj.cp.util.Utils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaseService {

    @Autowired
    private CrimeCaseClient crimeCaseClient;

    public List<CaseJudiciaryResult> getCaseById(Long id) {
        HttpEntity<String> result = crimeCaseClient.getCaseById(id);

        if (result.getBody() == null || result.getBody().isEmpty()) {
            throw new RuntimeException("Response body is null or empty");
        }

        try {
            List<CaseresultsSchema> caseresultsSchemaList = Utils.convertJsonStringToList(
                result.getBody(),
                CaseresultsSchema.class
            );
            return convertToCaseJudiciaryResult(caseresultsSchemaList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<CaseJudiciaryResult> convertToCaseJudiciaryResult(List<CaseresultsSchema> retResult) {
        return retResult.stream()
            .map(a -> new CaseJudiciaryResult(a.getResultText()))
            .collect(Collectors.toList());
    }
}
