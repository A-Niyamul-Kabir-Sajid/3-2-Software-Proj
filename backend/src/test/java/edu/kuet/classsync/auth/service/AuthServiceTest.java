package edu.kuet.classsync.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import edu.kuet.classsync.user.repository.RoleRepository;
import edu.kuet.classsync.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PasswordPolicy passwordPolicy;
    @Mock JwtService jwtService;
    @Mock TokenBlacklistService blacklist;
    @InjectMocks AuthService authService;

    private static User storedUser(boolean enabled) {
        return User.builder().name("Mim").email("mim@kuet.ac.bd").passwordHash("$hash")
                .enabled(enabled).role(new Role(RoleName.STUDENT)).build();
    }

    @Test
    void register_withValidData_savesUserWithHashedPassword() {
        given(userRepository.existsByEmail("mim@kuet.ac.bd")).willReturn(false);
        given(roleRepository.findByName(RoleName.STUDENT)).willReturn(Optional.of(new Role(RoleName.STUDENT)));
        given(passwordEncoder.encode("secret123")).willReturn("$bcrypt$hash");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserResponse res = authService.register(new RegisterRequest("Mim", " MIM@kuet.ac.bd ", "secret123", null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$bcrypt$hash");
        assertThat(saved.getValue().getEmail()).isEqualTo("mim@kuet.ac.bd");
        assertThat(res.roles()).containsExactly(RoleName.STUDENT);
    }

    @Test
    void register_withDuplicateEmail_throwsConflict() {
        given(userRepository.existsByEmail("mim@kuet.ac.bd")).willReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Mim", "mim@kuet.ac.bd", "secret123", null)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withTeacherRole_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Sir", "sir@kuet.ac.bd", "secret123", RoleName.TEACHER)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("TEACHER");
    }

    @Test
    void register_withWeakPassword_throwsBadRequest() {
        given(userRepository.existsByEmail(any())).willReturn(false);
        willThrow(new BadRequestException("weak")).given(passwordPolicy).validate("123");

        assertThatThrownBy(() -> authService.register(new RegisterRequest("Mim", "mim@kuet.ac.bd", "123", RoleName.CR)))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        given(userRepository.findByEmail("mim@kuet.ac.bd")).willReturn(Optional.of(storedUser(true)));
        given(passwordEncoder.matches("wrong", "$hash")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("mim@kuet.ac.bd", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withDisabledAccount_throwsDisabled() {
        given(userRepository.findByEmail("mim@kuet.ac.bd")).willReturn(Optional.of(storedUser(false)));
        given(passwordEncoder.matches("secret123", "$hash")).willReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("mim@kuet.ac.bd", "secret123")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void login_withValidCredentials_returnsBearerToken() {
        User user = storedUser(true);
        given(userRepository.findByEmail("mim@kuet.ac.bd")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("secret123", "$hash")).willReturn(true);
        given(jwtService.generate(user)).willReturn("jwt-token");
        given(jwtService.ttlSeconds()).willReturn(7200L);

        AuthResponse res = authService.login(new LoginRequest("MIM@kuet.ac.bd", "secret123"));

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.expiresIn()).isEqualTo(7200L);
    }
}
