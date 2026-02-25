package uk.gov.moj.cp.model.mock;

import lombok.Builder;
import lombok.Data;
import uk.gov.moj.cp.model.HearingType;

@Data
@Builder
public class MockDataSummary {

    HearingType hearingType;

    int months;

    int days;

    @Builder.Default
    int totalHearings = 1;

}
