package com.employee.loan_system.controller;

import com.employee.loan_system.auth.AuthService;
import com.employee.loan_system.auth.AuthCookieService;
import com.employee.loan_system.auth.dto.AuthResponse;
import com.employee.loan_system.auth.dto.LoginRequest;
import com.employee.loan_system.auth.dto.SessionResponse;
import com.employee.loan_system.auth.dto.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthCookieService authCookieService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.login(request);
            authCookieService.writeAuthenticationCookies(response, authResponse);
            return new ResponseEntity<>(SessionResponse.from(authResponse), HttpStatus.OK);
        } catch (BadCredentialsException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.refresh(authCookieService.extractRefreshToken(request));
            authCookieService.writeAuthenticationCookies(response, authResponse);
            return new ResponseEntity<>(SessionResponse.from(authResponse), HttpStatus.OK);
        } catch (RuntimeException ex) {
            authCookieService.clearAuthenticationCookies(response);
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(authCookieService.extractRefreshToken(request));
        authCookieService.clearAuthenticationCookies(response);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(Authentication authentication) {
        return new ResponseEntity<>(authService.me(authentication), HttpStatus.OK);
    }
}
