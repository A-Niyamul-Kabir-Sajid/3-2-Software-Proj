package edu.kuet.classsync.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps every exception to an {@link ApiError} JSON body. Stack traces are logged, never returned. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noRoute(NoResourceFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "No endpoint " + req.getMethod() + " " + req.getRequestURI(), req);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> badRequest(BadRequestException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ApiError.Builder body = base(HttpStatus.BAD_REQUEST, "Validation failed", req);
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> body.fieldError(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(body.build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> invalidParams(ConstraintViolationException ex, HttpServletRequest req) {
        ApiError.Builder body = base(HttpStatus.BAD_REQUEST, "Validation failed", req);
        ex.getConstraintViolations()
                .forEach(v -> body.fieldError(v.getPropertyPath().toString(), v.getMessage()));
        return ResponseEntity.badRequest().body(body.build());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> unreadable(Exception ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Malformed request", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> badCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Invalid email or password", req);
    }

    @ExceptionHandler(DisabledException.class)
    ResponseEntity<ApiError> disabled(DisabledException ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, "Account is disabled", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> unauthenticated(AuthenticationException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Authentication required", req);
    }

    @ExceptionHandler({AccessDeniedException.class, ForbiddenOperationException.class})
    ResponseEntity<ApiError> forbidden(RuntimeException ex, HttpServletRequest req) {
        String msg = ex instanceof ForbiddenOperationException ? ex.getMessage() : "Access denied";
        return respond(HttpStatus.FORBIDDEN, msg, req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req);
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(base(status, message, req).build());
    }

    private static ApiError.Builder base(HttpStatus status, String message, HttpServletRequest req) {
        return ApiError.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI());
    }
}
