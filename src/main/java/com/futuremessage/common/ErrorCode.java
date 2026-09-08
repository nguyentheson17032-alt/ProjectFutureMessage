package com.futuremessage.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired access token"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh token has expired"),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "Refresh token has already been used"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    NOT_ADMIN(HttpStatus.FORBIDDEN, "Admin role required"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "User account is disabled"),
    CANNOT_MODIFY_SELF_ROLE(HttpStatus.CONFLICT, "Cannot change your own role"),
    LAST_ADMIN(HttpStatus.CONFLICT, "Cannot modify the last remaining admin"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Message not found"),
    MESSAGE_LOCKED(HttpStatus.FORBIDDEN, "Message is still locked"),
    MESSAGE_NOT_EDITABLE(HttpStatus.CONFLICT, "Message can only be changed while locked"),
    MESSAGE_NOT_AVAILABLE(HttpStatus.CONFLICT, "Message is not available to open"),
    NOT_SENDER(HttpStatus.FORBIDDEN, "Only the sender can perform this action"),
    NOT_RECIPIENT(HttpStatus.FORBIDDEN, "Only the recipient can open this message"),
    UNLOCK_AT_MUST_BE_FUTURE(HttpStatus.BAD_REQUEST, "unlockAt must be in the future"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "The resource was modified by another request"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
