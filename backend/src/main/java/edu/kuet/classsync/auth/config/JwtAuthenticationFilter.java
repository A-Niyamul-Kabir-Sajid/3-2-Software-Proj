package edu.kuet.classsync.auth.config;

import edu.kuet.classsync.auth.jwt.JwtService;
import edu.kuet.classsync.auth.jwt.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying "Authorization: Bearer &lt;jwt&gt;". Invalid, expired, revoked tokens or
 * disabled users simply leave the request unauthenticated; the entry point then answers 401.
 * Not a Spring bean on purpose, so it is only registered inside the security filter chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService blacklist,
                                   UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parse(header.substring(BEARER.length()))
                    .filter(claims -> !blacklist.isRevoked(claims.getId()))
                    .ifPresent(claims -> authenticate(claims.getSubject(), request));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String email, HttpServletRequest request) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (!user.isEnabled()) {
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ignored) {
            // user was deleted after the token was issued: stay unauthenticated
        }
    }
}
