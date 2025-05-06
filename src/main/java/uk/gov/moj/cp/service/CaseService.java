package uk.gov.moj.cp.service;

import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.CaseJudiciaryResult;

import java.util.Arrays;
import java.util.List;

@Service
public class CaseService {

    public List<CaseJudiciaryResult> getCaseById(Long id) {
        return Arrays.asList(
            CaseJudiciaryResult.builder().result(id + " Guilty plea accepted by the court.").build(),
            CaseJudiciaryResult.builder().result(id + " Sentenced to 12 months custody.").build(),
            CaseJudiciaryResult.builder().result(id + " Fine of £500 imposed.").build());
    }
}
