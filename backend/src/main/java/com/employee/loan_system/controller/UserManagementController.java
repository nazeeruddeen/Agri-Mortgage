package com.employee.loan_system.controller;

import com.employee.loan_system.auth.UserManagementService;
import com.employee.loan_system.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/users")
@CrossOrigin(originPatterns = "http://localhost:*")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> listUsers() {
        return new ResponseEntity<>(userManagementService.getAllUsers(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<UserSummaryResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return new ResponseEntity<>(userManagementService.createUser(request), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserSummaryResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return new ResponseEntity<>(userManagementService.updateUserRole(id, request), HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserSummaryResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return new ResponseEntity<>(userManagementService.updateUserStatus(id, request), HttpStatus.OK);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        userManagementService.resetPassword(id, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
