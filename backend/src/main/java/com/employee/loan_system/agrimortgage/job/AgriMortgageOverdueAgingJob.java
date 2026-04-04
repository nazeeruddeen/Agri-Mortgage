package com.employee.loan_system.agrimortgage.job;

import com.employee.loan_system.agrimortgage.service.AgriMortgageServicingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AgriMortgageOverdueAgingJob {

    private static final Logger log = LoggerFactory.getLogger(AgriMortgageOverdueAgingJob.class);

    private final AgriMortgageServicingService servicingService;

    public AgriMortgageOverdueAgingJob(AgriMortgageServicingService servicingService) {
        this.servicingService = servicingService;
    }

    @Scheduled(
            cron = "${app.agri.servicing.overdue-scan-cron:0 15 0 * * *}",
            zone = "${app.agri.servicing.overdue-scan-zone:Asia/Kolkata}"
    )
    public void ageOverdueInstallments() {
        int aged = servicingService.ageOverdueInstallments(LocalDate.now());
        if (aged > 0) {
            log.warn("AgriMortgageOverdueAgingJob: marked {} installment(s) overdue", aged);
        }
    }
}
