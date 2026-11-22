package edu.kuet.classsync.support;

import edu.kuet.classsync.auth.jwt.JwtService;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.repository.RoleRepository;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates users with a given role and returns ready-to-use JWTs for integration tests in every module. */
@Component
public class TestAuthHelper {

    public static final String PASSWORD = "password123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TestAuthHelper(UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Persists a new user with a unique email and the given role. */
    public User createUser(RoleName role) {
        return userRepository.save(User.builder()
                .name(role.name().toLowerCase() + " tester")
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(roleRepository.findByName(role).orElseThrow())
                .build());
    }

    public String tokenFor(User user) {
        return jwtService.generate(user);
    }

    /** "Bearer &lt;jwt&gt;" for a brand-new user with the given role. */
    public String bearer(RoleName role) {
        return "Bearer " + tokenFor(createUser(role));
    }

    public String bearer(User user) {
        return "Bearer " + tokenFor(user);
    }
}
