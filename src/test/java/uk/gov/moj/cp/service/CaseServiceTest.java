package uk.gov.moj.cp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.moj.cp.client.CrimeCaseClient;
import uk.gov.moj.cp.dto.CaseJudiciaryResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {

    @InjectMocks
    private CaseService caseService;
    @Mock
    private CrimeCaseClient crimeCaseClient;

    @Test
    void testGetCaseById_ValidResponse_ReturnsCaseJudiciaryResultList() {
        Long caseId = 1L;
        String mockResponse = "[ { \"resultText\": \"Guilty plea accepted by the court.\" }, "
            + "{ \"resultText\": \"Sentenced to 12 months custody.\" }, "
            + "{ \"resultText\": \"Fine of £500 imposed.\" } ]";

        ResponseEntity<String> mockHttpEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(crimeCaseClient.getCaseById(caseId)).thenReturn(mockHttpEntity);
        List<CaseJudiciaryResult> result = caseService.getCaseById(caseId);

        assertEquals(3, result.size());
        assertEquals("Guilty plea accepted by the court.", result.get(0).resultText(),
            "Result content should match"
        );
    }

}
