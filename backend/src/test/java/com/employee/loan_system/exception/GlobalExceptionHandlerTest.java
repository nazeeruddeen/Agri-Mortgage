package com.employee.loan_system.exception;

import com.employee.loan_system.agrimortgage.entity.AgriMortgageApplication;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLockingFailureShouldReturnConflict() {
        var exception = new ObjectOptimisticLockingFailureException(AgriMortgageApplication.class, 42L);

        var response = handler.handleOptimisticLockException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body.get("status")).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.get("message")).isEqualTo("Concurrent update detected. Reload the record and retry.");
    }
}
