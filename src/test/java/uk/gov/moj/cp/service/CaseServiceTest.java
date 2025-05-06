package uk.gov.moj.cp.service;

import org.junit.jupiter.api.Test;
import uk.gov.moj.cp.dto.CaseJudiciaryResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CaseServiceTest {

    private final CaseService caseService = new CaseService();

    @Test
    void testGetCaseById_ReturnsCaseJudiciaryResult() {
        Long caseId = 1L;

        List<CaseJudiciaryResult> result = caseService.getCaseById(caseId);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Result list size should be 1");
        assertEquals("Sample CaseJudiciaryResult", result.get(0).getResult(), "Result content should match");
    }
}
