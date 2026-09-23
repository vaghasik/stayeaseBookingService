package com.apexon.stayease.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

/**
 * Unit tests for GlobalExceptionHandler
 * Tests exception handling and error responses
 *
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock private WebRequest webRequest;

    @Mock private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "id", "123");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).contains("User not found with id");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle EmailAlreadyExistsException")
    void testHandleEmailAlreadyExistsException() {
        EmailAlreadyExistsException exception =
                new EmailAlreadyExistsException("Email already exists");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleEmailAlreadyExistsException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(response.getBody().getMessage()).isEqualTo("Email already exists");
    }

    @Test
    @DisplayName("Should handle InvalidCredentialsException")
    void testHandleInvalidCredentialsException() {
        InvalidCredentialsException exception =
                new InvalidCredentialsException("Invalid credentials");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleAuthenticationException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    @DisplayName("Should handle BadCredentialsException")
    void testHandleBadCredentialsException() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleAuthenticationException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    @DisplayName("Should handle UsernameNotFoundException")
    void testHandleUsernameNotFoundException() {
        UsernameNotFoundException exception = new UsernameNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleAuthenticationException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    void testHandleValidationException() {
        FieldError fieldError = new FieldError("user", "email", "must not be null");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleValidationException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getDetails()).isNotNull();
        assertThat(response.getBody().getDetails()).containsKey("email");
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void testHandleGlobalException() {
        Exception exception = new Exception("Unexpected error");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleGlobalException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("Should include timestamp in error response")
    void testErrorResponse_IncludesTimestamp() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Hotel", "id", "1");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception, webRequest);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should include correct path in error response")
    void testErrorResponse_IncludesPath() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/auth/login");
        ResourceNotFoundException exception = new ResourceNotFoundException("Booking", "id", "99");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleResourceNotFoundException(exception, webRequest);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/auth/login");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400")
    void testHandleIllegalArgumentException() {
        IllegalArgumentException exception =
                new IllegalArgumentException("Check-out date must be after check-in date");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleIllegalArgumentException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Check-out date must be after check-in date");
    }

    @Test
    @DisplayName("Should handle IllegalStateException with 400")
    void testHandleIllegalStateException() {
        IllegalStateException exception =
                new IllegalStateException("Room is not available for the selected dates");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleIllegalStateException(exception, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("INVALID_STATE");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Room is not available for the selected dates");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with 403 and ACCESS_DENIED error")
    void testHandleAccessDeniedException_AccessDenied() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/forbidden");
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Denied");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleAccessDeniedException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().getMessage())
                .isEqualTo("You are not authorized to perform this action");
        assertThat(response.getBody().getPath()).isEqualTo("/api/forbidden");
    }

    @Test
    @DisplayName("Should handle AuthorizationDeniedException with 403 and ACCESS_DENIED error")
    void testHandleAccessDeniedException_AuthorizationDenied() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/another-forbidden");
        org.springframework.security.authorization.AuthorizationDeniedException ex =
                new org.springframework.security.authorization.AuthorizationDeniedException(
                        "Denied");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleAccessDeniedException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().getMessage())
                .isEqualTo("You are not authorized to perform this action");
        assertThat(response.getBody().getPath()).isEqualTo("/api/another-forbidden");
    }
}
