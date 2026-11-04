package com.mingle.a2srv_service.config;

import com.mingle.a2srv_service.entity.Role;
import com.mingle.a2srv_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes the user registered with ADMIN_EMAIL an admin at startup. It only promotes accounts that
 * already exist; registration never grants a role. Emails are not verified, so register the admin
 * account yourself before exposing the service, otherwise someone else could register that address
 * first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${admin.email:}")
    private String adminEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        userRepository.findByEmail(adminEmail.trim()).ifPresentOrElse(user -> {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                log.info("Promoted {} to ADMIN (ADMIN_EMAIL)", user.getUsername());
            }
        }, () -> log.warn("ADMIN_EMAIL is set but no user is registered with it yet; restart after registering"));
    }
}
