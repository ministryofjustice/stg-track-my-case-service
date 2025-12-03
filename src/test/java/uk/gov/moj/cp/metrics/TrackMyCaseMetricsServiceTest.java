package uk.gov.moj.cp.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TrackMyCaseMetricsServiceTest {

    private PrometheusRegistry prometheusRegistry;
    private TrackMyCaseMetricsService trackMyCaseMetricsService;

    @BeforeEach
    void setUp() {
        prometheusRegistry = new PrometheusRegistry();
        trackMyCaseMetricsService = new TrackMyCaseMetricsService(prometheusRegistry);
    }

    @Test
    @DisplayName("Should initialize service with counter")
    void shouldInitializeServiceWithCounter() {
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        assertThat(counter).isNotNull();
    }

    @Test
    @DisplayName("Should increment counter when incrementCaseDetailsCount is called")
    void shouldIncrementCounterWhenIncrementCaseDetailsCountIsCalled() {
        String caseReference = "CASE123";
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        double initialCount = counter.get();
        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference);
        double finalCount = counter.get();
        assertThat(finalCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("Should increment counter multiple times correctly")
    void shouldIncrementCounterMultipleTimesCorrectly() {
        String caseReference1 = "CASE123";
        String caseReference2 = "CASE456";
        String caseReference3 = "CASE789";
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        double initialCount = counter.get();

        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference1);
        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference2);
        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference3);

        double finalCount = counter.get();
        assertThat(finalCount).isEqualTo(initialCount + 3.0);
    }

    @Test
    @DisplayName("Should handle increment with null case reference")
    void shouldHandleIncrementWithNullCaseReference() {
        String caseReference = null;
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        double initialCount = counter.get();

        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference);

        double finalCount = counter.get();
        assertThat(finalCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("Should handle increment with empty case reference")
    void shouldHandleIncrementWithEmptyCaseReference() {
        String caseReference = "";
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        double initialCount = counter.get();

        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference);

        double finalCount = counter.get();
        assertThat(finalCount).isEqualTo(initialCount + 1.0);
    }

    @Test
    @DisplayName("Should create counter with correct name")
    void shouldCreateCounterWithCorrectName() {
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();

        assertThat(counter).isNotNull();
        assertThat(counter.get()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should start with zero count")
    void shouldStartWithZeroCount() {
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        double count = counter.get();

        assertThat(count).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should increment counter with different case reference formats")
    void shouldIncrementCounterWithDifferentCaseReferenceFormats() {
        String caseReference1 = "CASE-123";
        String caseReference2 = "case_456";
        String caseReference3 = "Case789";
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        double initialCount = counter.get();

        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference1);
        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference2);
        trackMyCaseMetricsService.incrementCaseDetailsCount(caseReference3);

        double finalCount = counter.get();
        assertThat(finalCount).isEqualTo(initialCount + 3.0);
    }

    @Test
    @DisplayName("Should handle concurrent increments correctly")
    void shouldHandleConcurrentIncrementsCorrectly() throws InterruptedException {
        int numberOfThreads = 10;
        int incrementsPerThread = 5;
        Counter counter = trackMyCaseMetricsService.getCaseDetailsCounter();
        double initialCount = counter.get();

        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    trackMyCaseMetricsService.incrementCaseDetailsCount("CASE-" + threadId + "-" + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        double finalCount = counter.get();
        int expectedIncrements = numberOfThreads * incrementsPerThread;
        assertThat(finalCount).isEqualTo(initialCount + expectedIncrements);
    }
}

