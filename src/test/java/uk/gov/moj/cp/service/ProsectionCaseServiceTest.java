package uk.gov.moj.cp.service;

import com.moj.generated.hmcts.ProsecutionCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.moj.cp.client.api.ProsecutionCaseClient;
import uk.gov.moj.cp.dto.outbound.ProsecutionCaseDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProsectionCaseServiceTest {

    @Mock
    private ProsecutionCaseClient prosecutionCaseClient;

    @InjectMocks
    private ProsectionCaseService prosectionCaseService;

    private final String accessToken = "testToken";
    private final String caseUrn = "URN12345";

    @Test
    void getCaseStatus_returnsMappedDto_whenClientReturnsBody() {
        ProsecutionCase caseStatus = new ProsecutionCase("Active", true);
        ResponseEntity<ProsecutionCase> entity = ResponseEntity.ok(caseStatus);
        when(prosecutionCaseClient.getCaseDetails(accessToken, caseUrn)).thenReturn(entity);

        ProsecutionCaseDTO dto = prosectionCaseService.getCaseStatus(accessToken, caseUrn);

        assertNotNull(dto);
        assertEquals("Active", dto.getCaseStatus());
        assertEquals(true, dto.isReportingRestrictions());
        verify(prosecutionCaseClient).getCaseDetails(eq(accessToken), eq(caseUrn));
    }

    @Test
    void getCaseStatus_returnsNull_whenResponseBodyIsNull() {
        when(prosecutionCaseClient.getCaseDetails(anyString(), anyString()))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        ProsecutionCaseDTO result = prosectionCaseService.getCaseStatus(accessToken, caseUrn);

        assertNull(result);
    }

    @Test
    void getCaseStatus_returnsNull_whenClientReturnsNullEntity() {
        when(prosecutionCaseClient.getCaseDetails(accessToken, caseUrn)).thenReturn(null);

        ProsecutionCaseDTO result = prosectionCaseService.getCaseStatus(accessToken, caseUrn);

        assertNull(result);
    }

    @Test
    void getCaseStatus_mapsNullFieldsFromCaseStatus_whenPresent() {
        ProsecutionCase caseStatus = new ProsecutionCase(null, false);
        when(prosecutionCaseClient.getCaseDetails(accessToken, caseUrn)).thenReturn(ResponseEntity.ok(caseStatus));

        ProsecutionCaseDTO dto = prosectionCaseService.getCaseStatus(accessToken, caseUrn);

        assertNotNull(dto);
        assertNull(dto.getCaseStatus());
        assertFalse(dto.isReportingRestrictions());
    }
}
