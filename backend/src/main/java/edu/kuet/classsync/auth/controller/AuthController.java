package edu.kuet.classsync.auth.controller;

import edu.kuet.classsync.auth.CurrentUserProvider;
import edu.kuet.classsync.auth.dto.AuthResponse;
import edu.kuet.classsync.auth.dto.LoginRequest;
import edu.kuet.classsync.auth.dto.RegisterRequest;
import edu.kuet.classsync.auth.service.AuthService;
import edu.kuet.classsync.user.dto.UserResponse;
import edu.kuet.classsync.user.mapper.UserMapper;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER = "Bearer ";

    private final AuthService authService;
    private final CurrentUserProvider currentUser;

    public AuthController(AuthService authService, CurrentUserProvider currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + created.id())).body(created);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        if (authorization.startsWith(BEARER)) {
            authService.logout(authorization.substring(BEARER.length()));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserMapper.toResponse(currentUser.get());
    }
}
