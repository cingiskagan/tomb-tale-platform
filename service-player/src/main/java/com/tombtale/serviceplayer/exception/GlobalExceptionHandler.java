package com.tombtale.serviceplayer.exception;

import com.tombtale.commons.web.PlatformExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Player's entry point into the shared error contract.
 *
 * <p>Empty by design: the annotation is the whole job. {@link
 * PlatformExceptionHandler} carries none, so a service opts in rather than
 * gaining an advice by being on the classpath. Domain exceptions would go
 * here; this service throws only {@code ResponseStatusException} so far.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends PlatformExceptionHandler {
}
