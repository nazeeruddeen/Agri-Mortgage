package com.employee.loan_system.auth;

import com.employee.loan_system.auth.dto.*;
import com.employee.loan_system.entity.AppUser;
import com.employee.loan_system.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserManagementService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityAuditService securityAuditService;

    public List<UserSummaryResponse> getAllUsers() {
        return appUserRepository.findAll().stream()
                .map(this::mapToSummary)
                .toList();
    }

    public UserSummaryResponse createUser(CreateUserRequest request) {
        String username = request.getUsername().trim();

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new RuntimeException("Username already exists");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setActive(true);

        AppUser savedUser = appUserRepository.save(user);

        securityAuditService.log(
                "CREATE_USER",
                savedUser.getId(),
                savedUser.getUsername(),
                "Created user with role=" + savedUser.getRole().name() + ", active=" + savedUser.isActive()
        );

        return mapToSummary(savedUser);
    }

    public UserSummaryResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        AppUser user = findUser(id);
        String previousRole = user.getRole().name();
        user.setRole(request.getRole());
        AppUser savedUser = appUserRepository.save(user);

        securityAuditService.log(
                "UPDATE_USER_ROLE",
                savedUser.getId(),
                savedUser.getUsername(),
                "Role changed from " + previousRole + " to " + savedUser.getRole().name()
        );

        return mapToSummary(savedUser);
    }

    public UserSummaryResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        AppUser user = findUser(id);
        boolean previousStatus = user.isActive();
        user.setActive(Boolean.TRUE.equals(request.getActive()));
        AppUser savedUser = appUserRepository.save(user);

        securityAuditService.log(
                "UPDATE_USER_STATUS",
                savedUser.getId(),
                savedUser.getUsername(),
                "Status changed from " + (previousStatus ? "ACTIVE" : "INACTIVE") + " to " + (savedUser.isActive() ? "ACTIVE" : "INACTIVE")
        );

        return mapToSummary(savedUser);
    }

    public void resetPassword(Long id, ResetPasswordRequest request) {
        AppUser user = findUser(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        AppUser savedUser = appUserRepository.save(user);

        securityAuditService.log(
                "RESET_USER_PASSWORD",
                savedUser.getId(),
                savedUser.getUsername(),
                "Password reset performed"
        );
    }

    private AppUser findUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    private UserSummaryResponse mapToSummary(AppUser user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .active(user.isActive())
                .build();
    }
}
