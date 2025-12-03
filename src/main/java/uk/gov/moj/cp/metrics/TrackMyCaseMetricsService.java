package uk.gov.moj.cp.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TrackMyCaseMetricsService {

    @Getter
    private final Counter caseDetailsCounter;

    public TrackMyCaseMetricsService(PrometheusRegistry meterRegistry) {
        this.caseDetailsCounter =
            Counter.builder()
                .name("trackmycase_service_started")
                .help("Total number of cases viewed in Track My Case service")
                .register(meterRegistry);
    }

    public void incrementCaseDetailsCount(String caseReference) {
        log.info("Case reference {} created", caseReference);
        caseDetailsCounter.inc();
    }

}
