package edu.kuet.classsync.auth.policy;

import edu.kuet.classsync.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

/** At least 8 characters with at least one letter and one digit. */
@Component
public class DefaultPasswordPolicy implements PasswordPolicy {

    static final int MIN_LENGTH = 8;

    @Override
    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new BadRequestException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (rawPassword.chars().noneMatch(Character::isLetter) || rawPassword.chars().noneMatch(Character::isDigit)) {
            throw new BadRequestException("Password must contain at least one letter and one digit");
        }
    }
}
