package com.employee.loan_system.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private long expiresIn;
    private String username;
    private String role;

    public static SessionResponse from(AuthResponse authResponse) {
        return SessionResponse.builder()
                .expiresIn(authResponse.getExpiresIn())
                .username(authResponse.getUsername())
                .role(authResponse.getRole())
                .build();
    }
}
