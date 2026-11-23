package edu.kuet.classsync.auth;

import edu.kuet.classsync.user.entity.User;
import edu.kuet.classsync.user.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Loads the logged-in {@link User} for ownership checks in any module's service layer. */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("User no longer exists"));
    }
}
