package com.employee.loan_system.auth;

import com.employee.loan_system.auth.dto.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    @Value("${app.security.cookies.access-token-name:agri_mortgage_access_token}")
    private String accessTokenCookieName;

    @Value("${app.security.cookies.refresh-token-name:agri_mortgage_refresh_token}")
    private String refreshTokenCookieName;

    @Value("${app.security.cookies.secure:false}")
    private boolean secureCookies;

    @Value("${app.security.cookies.same-site:Strict}")
    private String sameSite;

    @Value("${app.security.cookies.domain:}")
    private String cookieDomain;

    @Value("${app.security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public void writeAuthenticationCookies(HttpServletResponse response, AuthResponse authResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(accessTokenCookieName, authResponse.getAccessToken(),
                Duration.ofSeconds(authResponse.getExpiresIn())).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshTokenCookieName, authResponse.getRefreshToken(),
                Duration.ofMillis(refreshTokenExpirationMs)).toString());
    }

    public void clearAuthenticationCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(accessTokenCookieName, "", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshTokenCookieName, "", Duration.ZERO).toString());
    }

    public String extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, accessTokenCookieName);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, refreshTokenCookieName);
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }
}
