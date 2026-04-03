package com.employee.loan_system.security;

import com.employee.loan_system.entity.AppUser;
import com.employee.loan_system.entity.UserRole;
import com.employee.loan_system.repository.AppUserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(name = "app.security.bootstrap-users.enabled", havingValue = "true")
public class SecurityDataInitializer implements CommandLineRunner {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public SecurityDataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        ensureUser(
                environment.getProperty("app.security.bootstrap.admin.username", "admin"),
                requiredProperty("app.security.bootstrap.admin.password"),
                UserRole.ADMIN
        );
        ensureUser(
                environment.getProperty("app.security.bootstrap.officer.username", "officer"),
                requiredProperty("app.security.bootstrap.officer.password"),
                UserRole.LOAN_OFFICER
        );
        ensureUser(
                environment.getProperty("app.security.bootstrap.reviewer.username", "reviewer"),
                requiredProperty("app.security.bootstrap.reviewer.password"),
                UserRole.REVIEWER
        );
        ensureUser(
                environment.getProperty("app.security.bootstrap.borrower.username", "borrower"),
                requiredProperty("app.security.bootstrap.borrower.password"),
                UserRole.BORROWER
        );
    }

    private void ensureUser(String username, String password, UserRole role) {
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        appUserRepository.save(user);

        System.out.println("[AUTH-INIT] Default user created -> username: " + username + ", role: " + role.name());
    }

    private String requiredProperty(String propertyName) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required bootstrap security property: " + propertyName);
        }
        return value;
    }
}
