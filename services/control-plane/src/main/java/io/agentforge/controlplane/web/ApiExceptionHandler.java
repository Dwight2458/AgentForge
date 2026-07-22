package io.agentforge.controlplane.web;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.agentforge.controlplane.service.NotFoundException;
import io.agentforge.controlplane.github.GithubApiException;
import io.agentforge.controlplane.github.GithubAuthenticationRequiredException;
import io.agentforge.controlplane.github.GithubConfigurationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiDtos.ApiError> notFound(NotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<ApiDtos.ApiError> conflict(RuntimeException exception) {
        return error(HttpStatus.CONFLICT, "WORKFLOW_CONFLICT", exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiDtos.ApiError> invalid(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details);
    }

    @ExceptionHandler(GithubAuthenticationRequiredException.class)
    ResponseEntity<ApiDtos.ApiError> githubAuthentication(GithubAuthenticationRequiredException exception) {
        return error(HttpStatus.UNAUTHORIZED, "GITHUB_AUTHENTICATION_REQUIRED", exception.getMessage(), List.of());
    }

    @ExceptionHandler(GithubConfigurationException.class)
    ResponseEntity<ApiDtos.ApiError> githubConfiguration(GithubConfigurationException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "GITHUB_NOT_CONFIGURED", exception.getMessage(), List.of());
    }

    @ExceptionHandler(GithubApiException.class)
    ResponseEntity<ApiDtos.ApiError> githubApi(GithubApiException exception) {
        List<String> details = exception.githubStatus() == 0
                ? List.of()
                : List.of("GitHub status: " + exception.githubStatus());
        return error(HttpStatus.BAD_GATEWAY, "GITHUB_API_ERROR", exception.getMessage(), details);
    }

    private ResponseEntity<ApiDtos.ApiError> error(
            HttpStatus status, String code, String message, List<String> details) {
        return ResponseEntity.status(status).body(new ApiDtos.ApiError(code, message, details, Instant.now()));
    }
}
