package uk.gov.moj.cp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.moj.generated.hmcts.Judiciary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.client.JudgesClient;
import uk.gov.moj.cp.dto.JudgesResult;
import uk.gov.moj.cp.util.Utils;

import static uk.gov.moj.cp.util.Utils.getJsonNode;

@Service
public class JudgesService {

    @Autowired
    private JudgesClient judgesClient;

    public JudgesResult getCaseById(Long id) {
        HttpEntity<String> result = judgesClient.getJudgesById(id);

        if (result.getBody() == null || result.getBody().isEmpty()) {
            throw new RuntimeException("Response body is null or empty");
        }

        try {
            JsonNode judiciary = getJsonNode(result.getBody(), "judiciary");
            Judiciary judiciaryResult = Utils.convertJsonStringToType(
                judiciary.toString(),
                Judiciary.class
            );
            return convertToJudiciaryResult(judiciaryResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private JudgesResult convertToJudiciaryResult(Judiciary judiciaryResult) {
        return new JudgesResult(judiciaryResult.getJohTitle(),
            judiciaryResult.getJohNameSurname(), judiciaryResult.getRole().name(),
            judiciaryResult.getJohKnownAs());
    }
}
