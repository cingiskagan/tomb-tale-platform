package com.tombtale.servicecommerce.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

/**
 * For tests that write with no caller: every row they write names {@link #AUTHOR}.
 * A test that asserts who wrote a row extends {@link PostgresTestBase} and keeps the real auditor.
 */
public abstract class FixedAuthorTestBase extends PostgresTestBase {

    protected static final UUID AUTHOR = UUID.fromString("aaaaaaaa-0000-4000-8000-0000000000ad");

    @MockitoBean
    private AuditorAware<UUID> auditorAware;

    @BeforeEach
    protected void writeAsTheFixedAuthor() {
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(AUTHOR));
    }
}
