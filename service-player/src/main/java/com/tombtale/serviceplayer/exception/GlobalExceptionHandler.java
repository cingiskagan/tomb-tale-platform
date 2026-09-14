package com.tombtale.serviceplayer.exception;

import com.tombtale.commons.web.PlatformExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Player service's entry point into the shared error contract.
 *
 * <p>
 * Every handler it needs today is inherited from
 * {@link PlatformExceptionHandler}. The class exists to register them: an
 * advice is found by its {@code @RestControllerAdvice} annotation, and the
 * base class in {@code platform-commons} deliberately carries none, so a
 * service opts in rather than gaining an advice by being on the classpath.
 *
 * <p>
 * Registering it is what stops this service answering with Spring's default
 * error body, which drops the reason from every
 * {@code ResponseStatusException} the service layer throws — the 404 from
 * {@code updateMyProfile} arrived with an empty message before this.
 *
 * <p>
 * Service-specific exception types would go here. There are none yet: the dead
 * {@code PlayerNotFoundException} was deleted rather than revived, because it
 * took the internal {@code Long id} that ADR 0013 keeps out of anything
 * API-facing, and {@code ResponseStatusException} already lands in the same
 * envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends PlatformExceptionHandler {
}
