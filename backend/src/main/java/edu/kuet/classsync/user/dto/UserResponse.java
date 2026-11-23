package edu.kuet.classsync.user.dto;

import edu.kuet.classsync.user.entity.RoleName;
import java.time.Instant;
import java.util.Set;

/** Public view of a user. Never contains the password hash. */
public record UserResponse(Long id, String name, String email, Set<RoleName> roles, boolean enabled, Instant createdAt) { }
