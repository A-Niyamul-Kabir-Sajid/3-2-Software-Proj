package edu.kuet.classsync.common.config;

import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.repository.RoleRepository;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds the four roles and the initial ADMIN account (from env vars) on startup. Idempotent. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password:}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = User.normalizeEmail(adminEmail);
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Arrays.stream(RoleName.values())
                .filter(name -> roleRepository.findByName(name).isEmpty())
                .forEach(name -> roleRepository.save(new Role(name)));

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD is not set; skipping creation of the initial admin account");
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        userRepository.save(User.builder()
                .name("Administrator")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(adminRole)
                .build());
        log.info("Created initial admin account {}", adminEmail);
    }
}
