package com.employee.loan_system.agrimortgage.config;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplicationStatus;
import com.employee.loan_system.agrimortgage.entity.EncumbranceVerificationStatus;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageApplicationRepository;
import com.employee.loan_system.agrimortgage.repository.AgriMortgageDocumentRepository;
import com.employee.loan_system.agrimortgage.repository.AgriculturalLandParcelRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class AgriMortgageMetricsConfiguration {

    @Bean
    public MeterBinder agriMortgageWorkflowMetricsBinder(
            AgriMortgageApplicationRepository applicationRepository,
            AgriMortgageDocumentRepository documentRepository,
            AgriculturalLandParcelRepository landParcelRepository) {
        return registry -> {
            Gauge.builder("agri.mortgage.applications.count", applicationRepository::count)
                    .description("Total agri mortgage applications")
                    .register(registry);

            Gauge.builder("agri.mortgage.applications.eligible.count", applicationRepository::countByEligibleTrue)
                    .description("Eligible agri mortgage applications")
                    .register(registry);

            for (AgriMortgageApplicationStatus status : AgriMortgageApplicationStatus.values()) {
                Gauge.builder("agri.mortgage.applications.status.count",
                                () -> applicationRepository.countByStatus(status))
                        .description("Agri mortgage applications by workflow status")
                        .tag("status", status.name().toLowerCase())
                        .register(registry);
            }

            for (EncumbranceVerificationStatus status : EncumbranceVerificationStatus.values()) {
                Gauge.builder("agri.mortgage.encumbrance.status.count",
                                () -> applicationRepository.countByEncumbranceVerificationStatus(status))
                        .description("Agri mortgage applications by encumbrance verification state")
                        .tag("status", status.name().toLowerCase())
                        .register(registry);
            }

            Gauge.builder("agri.mortgage.documents.count", documentRepository::countApplicationsWithVerifiedRequiredDocuments)
                    .description("Applications with all required land and legal documents verified")
                    .tag("state", "ready")
                    .register(registry);

            Gauge.builder("agri.mortgage.documents.count",
                            () -> Math.max(applicationRepository.count() - documentRepository.countApplicationsWithVerifiedRequiredDocuments(), 0L))
                    .description("Applications still missing one or more required verified documents")
                    .tag("state", "pending")
                    .register(registry);

            Gauge.builder("agri.mortgage.land.parcels.count", landParcelRepository::countAllParcels)
                    .description("Total agricultural land parcels linked to applications")
                    .register(registry);

            Gauge.builder("agri.mortgage.amount", () -> decimalValue(applicationRepository.sumRequestedAmount()))
                    .description("Agri mortgage portfolio amount snapshots")
                    .tag("kind", "requested_total")
                    .baseUnit("currency")
                    .register(registry);

            Gauge.builder("agri.mortgage.amount",
                            () -> {
                                long totalApplications = applicationRepository.count();
                                if (totalApplications == 0) {
                                    return 0.0d;
                                }
                                return decimalValue(applicationRepository.sumRequestedAmount()) / totalApplications;
                            })
                    .description("Agri mortgage portfolio amount snapshots")
                    .tag("kind", "requested_average")
                    .baseUnit("currency")
                    .register(registry);

            Gauge.builder("agri.mortgage.amount", () -> decimalValue(landParcelRepository.sumTotalAppraisedValue()))
                    .description("Agri mortgage portfolio amount snapshots")
                    .tag("kind", "appraised_total")
                    .baseUnit("currency")
                    .register(registry);
        };
    }

    private double decimalValue(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }
}
