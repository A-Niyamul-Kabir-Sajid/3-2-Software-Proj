package edu.kuet.classsync.auth.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.kuet.classsync.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class DefaultPasswordPolicyTest {

    private final PasswordPolicy policy = new DefaultPasswordPolicy();

    @Test
    void validate_tooShort_isRejected() {
        assertThatThrownBy(() -> policy.validate("ab1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 8");
    }

    @Test
    void validate_withoutDigit_isRejected() {
        assertThatThrownBy(() -> policy.validate("onlyletters"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("digit");
    }

    @Test
    void validate_lettersAndDigits_isAccepted() {
        assertThatCode(() -> policy.validate("secret123")).doesNotThrowAnyException();
    }
}
