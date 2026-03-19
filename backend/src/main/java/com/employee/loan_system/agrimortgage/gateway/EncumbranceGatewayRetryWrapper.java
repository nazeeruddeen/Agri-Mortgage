package com.employee.loan_system.agrimortgage.gateway;

import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayClient.EncumbranceCheckResult;
import com.employee.loan_system.agrimortgage.gateway.EncumbranceGatewayClient.EncumbranceCheckStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Retry wrapper for EncumbranceGatewayClient with exponential backoff.
 *
 * Interview story:
 * "The revenue department API has occasional latency spikes and timeouts. I wrapped the
 *  gateway call in a manual exponential-backoff retry (1s → 2s → 4s, max 3 attempts).
 *  We only retry on GATEWAY_ERROR results — not on CLEAR or ENCUMBERED, which are definitive.
 *  If all attempts fail, we return a GATEWAY_ERROR result and the application stays in its
 *  current state for a supervisor to manually trigger a re-check."
 */
@Component
public class EncumbranceGatewayRetryWrapper {

    private static final Logger log = LoggerFactory.getLogger(EncumbranceGatewayRetryWrapper.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 1000L; // 1 second

    private final EncumbranceGatewayClient gatewayClient;

    public EncumbranceGatewayRetryWrapper(EncumbranceGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    /**
     * Calls checkEncumbrance with exponential backoff.
     * Retries ONLY on GATEWAY_ERROR — definitive results (CLEAR/ENCUMBERED/PENDING) are returned immediately.
     *
     * @param surveyNumber survey number of the land parcel
     * @param district     district where the parcel is registered
     * @return the encumbrance check result (may be GATEWAY_ERROR after exhausting retries)
     */
    public EncumbranceCheckResult checkWithRetry(String surveyNumber, String district) {
        EncumbranceCheckResult result = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                result = gatewayClient.checkEncumbrance(surveyNumber, district);
                if (result.status() != EncumbranceCheckStatus.GATEWAY_ERROR) {
                    // Definitive result — no need to retry
                    log.info("EncumbranceGatewayRetryWrapper: got {} on attempt {} for surveyNumber={}",
                            result.status(), attempt, surveyNumber);
                    return result;
                }
                log.warn("EncumbranceGatewayRetryWrapper: GATEWAY_ERROR on attempt {} for surveyNumber={}",
                        attempt, surveyNumber);
            } catch (Exception e) {
                log.warn("EncumbranceGatewayRetryWrapper: exception on attempt {} for surveyNumber={}: {}",
                        attempt, surveyNumber, e.getMessage());
            }

            if (attempt < MAX_ATTEMPTS) {
                long delayMs = BASE_DELAY_MS * (1L << (attempt - 1)); // 1s, 2s, 4s
                log.info("EncumbranceGatewayRetryWrapper: waiting {}ms before retry {} of {}",
                        delayMs, attempt + 1, MAX_ATTEMPTS);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("EncumbranceGatewayRetryWrapper: retry sleep interrupted");
                    break;
                }
            }
        }

        log.error("EncumbranceGatewayRetryWrapper: all {} attempts exhausted for surveyNumber={}", MAX_ATTEMPTS, surveyNumber);
        // Return last result (GATEWAY_ERROR) or synthesize one if all threw exceptions
        return result != null ? result : new EncumbranceCheckResult(
                surveyNumber, district,
                EncumbranceCheckStatus.GATEWAY_ERROR,
                "Gateway unavailable after " + MAX_ATTEMPTS + " attempts",
                false
        );
    }
}
