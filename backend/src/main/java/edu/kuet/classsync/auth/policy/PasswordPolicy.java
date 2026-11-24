package edu.kuet.classsync.auth.policy;

/**
 * Strategy pattern: a pluggable rule set for acceptable passwords.
 * Swap the implementation (e.g. a stricter policy for production) without touching AuthService.
 */
public interface PasswordPolicy {

    /** @throws edu.kuet.classsync.common.exception.BadRequestException if the password is not acceptable */
    void validate(String rawPassword);
}
