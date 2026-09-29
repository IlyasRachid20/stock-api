package com.ilyas.stockapi.security;

import com.ilyas.stockapi.entity.AppUser;
import com.ilyas.stockapi.entity.Role;
import com.ilyas.stockapi.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * On first start (no users at all) creates the admin account, so someone can log in and
 * create the other users. The password comes from APP_ADMIN_PASSWORD, or is generated and
 * shown once in the logs. Nothing happens once any user exists.
 */
@Component
@Order(1) // before DemoDataLoader, which creates the demo account
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    public AdminAccountInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            SecurityProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        String username = properties.adminUsername().trim().toLowerCase(Locale.ROOT);
        String password = properties.adminPassword();
        boolean generated = (password == null || password.isBlank());
        if (generated) {
            byte[] random = new byte[18];
            new SecureRandom().nextBytes(random);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        }

        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        if (generated) {
            log.warn("Created user '{}' with the generated password: {}  (shown only once; set APP_ADMIN_PASSWORD to choose it)",
                    username, password);
        } else {
            log.info("Created user '{}' with the password from APP_ADMIN_PASSWORD", username);
        }
    }
}
