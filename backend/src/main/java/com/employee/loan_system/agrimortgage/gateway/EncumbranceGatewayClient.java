package com.employee.loan_system.agrimortgage.gateway;

/**
 * Contract for querying a land registry / revenue department API
 * to check whether a land parcel has any encumbrances.
 *
 * Interview story:
 * "I designed the EncumbranceGatewayClient as an interface so that the domain logic
 *  is fully decoupled from the actual third-party integration. During development and testing
 *  we use a MockEncumbranceGatewayClient that returns deterministic responses.
 *  In production, we swap in the real HTTP client via Spring's @Primary or profile-based beans.
 *  This follows the Dependency Inversion Principle and keeps our service layer testable."
 */
public interface EncumbranceGatewayClient {

    /**
     * Checks the encumbrance status of a land parcel.
     *
     * @param surveyNumber  unique survey number of the land parcel
     * @param district      district where the land is registered
     * @return result containing the encumbrance status and any details
     */
    EncumbranceCheckResult checkEncumbrance(String surveyNumber, String district);

    /**
     * Result returned by the encumbrance gateway.
     */
    record EncumbranceCheckResult(
            String surveyNumber,
            String district,
            EncumbranceCheckStatus status,
            String encumbranceDetails,
            boolean gatewayAvailable
    ) {}

    enum EncumbranceCheckStatus {
        CLEAR,
        ENCUMBERED,
        PENDING_VERIFICATION,
        GATEWAY_ERROR
    }
}
