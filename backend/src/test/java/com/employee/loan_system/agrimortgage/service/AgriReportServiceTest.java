package com.employee.loan_system.agrimortgage.service;

import com.employee.loan_system.agrimortgage.repository.AgriMortgageApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgriReportServiceTest {

    @Mock
    private AgriMortgageApplicationRepository applicationRepository;

    @Test
    void getDistrictSummaryShouldUseDatabaseAggregationRows() {
        AgriReportService service = new AgriReportService(applicationRepository);
        when(applicationRepository.findDistrictSummaryRows()).thenReturn(List.of(
                new Object[] {"Nagpur", BigInteger.valueOf(12), BigInteger.valueOf(3), new BigDecimal("1450000.5"), new BigDecimal("0.47215")},
                new Object[] {"Amravati", BigInteger.valueOf(7), BigInteger.valueOf(2), new BigDecimal("980000"), new BigDecimal("0.5123")}
        ));

        List<AgriReportService.DistrictSummary> summaries = service.getDistrictSummary();

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).district()).isEqualTo("Nagpur");
        assertThat(summaries.get(0).totalApplications()).isEqualTo(12);
        assertThat(summaries.get(0).sanctionedApplications()).isEqualTo(3);
        assertThat(summaries.get(0).totalSanctionedAmount()).isEqualByComparingTo("1450000.50");
        assertThat(summaries.get(0).averageLtvRatio()).isEqualByComparingTo("0.4722");
        assertThat(summaries.get(1).district()).isEqualTo("Amravati");
        verify(applicationRepository).findDistrictSummaryRows();
        verify(applicationRepository, never()).findAll();
    }
}
