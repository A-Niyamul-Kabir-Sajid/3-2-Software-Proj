package edu.kuet.classsync.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.kuet.classsync.user.entity.Role;
import edu.kuet.classsync.user.entity.RoleName;
import edu.kuet.classsync.user.entity.User;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-bytes!";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    private static User user() {
        User u = User.builder().name("Rafi").email("Rafi@Example.com").passwordHash("hash")
                .role(new Role(RoleName.CR)).role(new Role(RoleName.STUDENT)).build();
        u.setId(7L);
        return u;
    }

    @Test
    void generateThenParse_returnsSameEmailUidAndRoles() {
        Claims claims = jwtService.parse(jwtService.generate(user())).orElseThrow();

        assertThat(claims.getSubject()).isEqualTo("rafi@example.com");
        assertThat(claims.get("uid", Long.class)).isEqualTo(7L);
        assertThat(claims.get("roles", List.class)).containsExactly("CR", "STUDENT");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void parse_tamperedToken_returnsEmpty() {
        String token = jwtService.generate(user());
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void parse_tokenSignedWithOtherSecret_returnsEmpty() {
        String foreign = new JwtService("another-secret-key-that-is-also-32-bytes-long", 60).generate(user());

        assertThat(jwtService.parse(foreign)).isEmpty();
    }

    @Test
    void constructor_shortSecret_failsFast() {
        assertThatThrownBy(() -> new JwtService("too-short", 60)).isInstanceOf(IllegalStateException.class);
    }
}
