package com.employee.loan_system.agrimortgage.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of EncumbranceGatewayClient for local development and testing.
 *
 * Interview story:
 * "Before the real revenue department API was available, I modeled the integration as an interface
 *  and built a mock that returns deterministic responses. This let us complete the full workflow
 *  end-to-end in dev/test without a real external dependency. When the real API became available,
 *  we just swapped the bean via Spring profiles — zero changes to domain logic."
 *
 * Profile override: set spring.profiles.active=production to swap in the real client.
 */
@Component
@Primary
public class MockEncumbranceGatewayClient implements EncumbranceGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(MockEncumbranceGatewayClient.class);

    // Simulated delay to test gateway latency handling
    private static final long SIMULATED_LATENCY_MS = 50;

    @Override
    public EncumbranceCheckResult checkEncumbrance(String surveyNumber, String district) {
        log.info("MockEncumbranceGatewayClient.checkEncumbrance called: surveyNumber={} district={}",
                surveyNumber, district);
        try {
            Thread.sleep(SIMULATED_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Deterministic rules for mock:
        // Survey numbers ending in "ENC" return ENCUMBERED (for test scenarios)
        // Survey numbers ending in "ERR" simulate a gateway error
        // All others return CLEAR
        if (surveyNumber != null && surveyNumber.toUpperCase().endsWith("ENC")) {
            log.info("MockEncumbranceGatewayClient: returning ENCUMBERED for surveyNumber={}", surveyNumber);
            return new EncumbranceCheckResult(
                    surveyNumber, district,
                    EncumbranceCheckStatus.ENCUMBERED,
                    "Mock: parcel has a registered mortgage lien",
                    true
            );
        }
        if (surveyNumber != null && surveyNumber.toUpperCase().endsWith("ERR")) {
            log.warn("MockEncumbranceGatewayClient: simulating GATEWAY_ERROR for surveyNumber={}", surveyNumber);
            return new EncumbranceCheckResult(
                    surveyNumber, district,
                    EncumbranceCheckStatus.GATEWAY_ERROR,
                    "Mock: simulated gateway unavailability",
                    false
            );
        }

        log.info("MockEncumbranceGatewayClient: returning CLEAR for surveyNumber={}", surveyNumber);
        return new EncumbranceCheckResult(
                surveyNumber, district,
                EncumbranceCheckStatus.CLEAR,
                null,
                true
        );
    }
}
