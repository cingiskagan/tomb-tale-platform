package com.tombtale.servicecommerce.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the documented gap: commerce cannot attribute a write to anyone yet.
 *
 * <p>When the lookup that closes it lands, this test is the one that has to
 * change, which is the point of having it.
 */
class CommerceAuditorAwareTest {

    @Test
    @DisplayName("commerce cannot yet name the principal behind a write")
    void resolvesNoAuditor() {
        assertThat(new CommerceAuditorAware().getCurrentAuditor()).isEmpty();
    }
}
