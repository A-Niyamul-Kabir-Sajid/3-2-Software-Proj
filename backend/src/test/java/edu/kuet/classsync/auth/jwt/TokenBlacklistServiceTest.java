package edu.kuet.classsync.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TokenBlacklistServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private final TokenBlacklistService blacklist = new TokenBlacklistService(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void isRevoked_afterRevoke_returnsTrue() {
        blacklist.revoke("jti-1", NOW.plusSeconds(600));

        assertThat(blacklist.isRevoked("jti-1")).isTrue();
        assertThat(blacklist.isRevoked("other")).isFalse();
    }

    @Test
    void isRevoked_whenTokenAlreadyExpired_returnsFalse() {
        blacklist.revoke("old", NOW.minusSeconds(1));

        assertThat(blacklist.isRevoked("old")).isFalse();
    }
}
