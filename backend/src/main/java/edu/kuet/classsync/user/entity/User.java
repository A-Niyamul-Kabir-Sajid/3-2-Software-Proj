package edu.kuet.classsync.user.entity;

import edu.kuet.classsync.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Application user. Many-to-many with {@link Role} through the {@code user_roles} join table.
 * Built with the Builder pattern: {@code User.builder().name(..).email(..).passwordHash(..).role(r).build()}.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() { }

    private User(Builder b) {
        this.name = b.name;
        this.email = normalizeEmail(b.email);
        this.passwordHash = b.passwordHash;
        this.enabled = b.enabled;
        this.roles = new HashSet<>(b.roles);
    }

    public static Builder builder() { return new Builder(); }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean hasRole(RoleName role) {
        return roles.stream().anyMatch(r -> r.getName() == role);
    }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getPasswordHash() { return passwordHash; }

    public boolean isEnabled() { return enabled; }

    public Set<Role> getRoles() { return Set.copyOf(roles); }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void replaceRoles(Collection<Role> newRoles) {
        roles.clear();
        roles.addAll(newRoles);
    }

    /** Builder pattern: readable, validated construction of a {@link User}. */
    public static final class Builder {
        private String name;
        private String email;
        private String passwordHash;
        private boolean enabled = true;
        private final Set<Role> roles = new HashSet<>();

        private Builder() { }

        public Builder name(String name) { this.name = name; return this; }

        public Builder email(String email) { this.email = email; return this; }

        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }

        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }

        public Builder role(Role role) { this.roles.add(role); return this; }

        public User build() {
            Objects.requireNonNull(name, "name is required");
            Objects.requireNonNull(email, "email is required");
            Objects.requireNonNull(passwordHash, "passwordHash is required");
            if (roles.isEmpty()) {
                throw new IllegalStateException("a user needs at least one role");
            }
            return new User(this);
        }
    }
}
