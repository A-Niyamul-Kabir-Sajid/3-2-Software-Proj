package edu.kuet.classsync.auth.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Logout support for stateless JWTs: revoked token ids (jti) are kept in memory until the token would
 * have expired anyway. Trade-off: the list is per instance and lost on restart, which is acceptable for a
 * single Render instance; a shared store (Redis/DB table) would be needed when scaling out.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklistService() { this(Clock.systemUTC()); }

    TokenBlacklistService(Clock clock) { this.clock = clock; }

    public void revoke(String jti, Instant expiresAt) {
        purgeExpired();
        revoked.put(jti, expiresAt);
    }

    public boolean isRevoked(String jti) {
        Instant exp = revoked.get(jti);
        if (exp == null) {
            return false;
        }
        if (exp.isBefore(clock.instant())) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        revoked.values().removeIf(exp -> exp.isBefore(now));
    }
}
