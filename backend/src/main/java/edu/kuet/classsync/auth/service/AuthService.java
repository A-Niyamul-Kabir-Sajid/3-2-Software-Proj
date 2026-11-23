package edu.kuet.classsync.auth.service;

import edu.kuet.classsync.auth.dto.AuthResponse;
import edu.kuet.classsync.auth.dto.LoginRequest;
import edu.kuet.classsync.auth.dto.RegisterRequest;
import edu.kuet.classsync.auth.jwt.JwtService;
import edu.kuet.classsync.auth.jwt.TokenBlacklistService;
import edu.kuet.classsync.auth.policy.PasswordPolicy;
import edu.kuet.classsync.common.exception.BadRequestException;
import edu.kuet.classsync.common.exception.ConflictException;
import edu.kuet.classsync.user.dto.UserResponse;
import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.mapper.UserMapper;
import edu.kuet.classsync.user.repository.RoleRepository;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /** Roles a user may pick when registering; TEACHER and ADMIN are granted by an ADMIN. */
    static final Set<RoleName> SELF_ASSIGNABLE = EnumSet.of(RoleName.STUDENT, RoleName.CR);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy, JwtService jwtService, TokenBlacklistService blacklist) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        RoleName roleName = req.role() == null ? RoleName.STUDENT : req.role();
        if (!SELF_ASSIGNABLE.contains(roleName)) {
            throw new BadRequestException("Role " + roleName + " cannot be self-assigned");
        }
        String email = User.normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " is already registered");
        }
        passwordPolicy.validate(req.password());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role " + roleName + " is not seeded"));

        User user = User.builder()
                .name(req.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .build();
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(User.normalizeEmail(req.email()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        return AuthResponse.bearer(jwtService.generate(user), jwtService.ttlSeconds(), UserMapper.toResponse(user));
    }

    /** Revokes the given token so it can no longer be used, even before it expires. */
    public void logout(String token) {
        jwtService.parse(token).ifPresent(claims ->
                blacklist.revoke(claims.getId(), claims.getExpiration().toInstant()));
    }
}
