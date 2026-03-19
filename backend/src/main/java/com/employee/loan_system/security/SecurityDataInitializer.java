package com.employee.loan_system.security;

import com.employee.loan_system.entity.AppUser;
import com.employee.loan_system.entity.UserRole;
import com.employee.loan_system.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityDataInitializer implements CommandLineRunner {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureUser("admin", "Admin@123", UserRole.ADMIN);
        ensureUser("officer", "Officer@123", UserRole.LOAN_OFFICER);
        ensureUser("reviewer", "Reviewer@123", UserRole.REVIEWER);
        ensureUser("borrower", "Borrower@123", UserRole.BORROWER);
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
}
