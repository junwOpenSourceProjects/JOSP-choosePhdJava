package com.choosephd.config;

import com.choosephd.entity.UserAccount;
import com.choosephd.repository.UserAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitRunner.class);

    private final ChoosePhdProperties properties;
    private final UserAccountMapper userAccountMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminInitRunner(ChoosePhdProperties properties, UserAccountMapper userAccountMapper) {
        this.properties = properties;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void run(String... args) {
        ChoosePhdProperties.Admin admin = properties.getAdmin();
        if (admin == null || admin.getUsername() == null || admin.getPassword() == null) {
            log.info("Admin credentials not configured, skip default admin creation");
            return;
        }

        String username = admin.getUsername().trim();
        if (username.isEmpty()) {
            log.info("Admin username is empty, skip default admin creation");
            return;
        }

        UserAccount existing = userAccountMapper.selectByUsername(username);
        if (existing != null) {
            log.info("Admin user '{}' already exists, skip creation", username);
            return;
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(admin.getPassword()));
        user.setRole("ROLE_ADMIN");
        userAccountMapper.insert(user);
        log.info("Created default admin user: {}", username);
    }
}
