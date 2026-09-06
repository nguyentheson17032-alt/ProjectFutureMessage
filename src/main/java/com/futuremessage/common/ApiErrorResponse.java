package com.futuremessage.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorDetail> fieldErrors
) {

    public record FieldErrorDetail(String field, String message) {
    }

    public static ApiErrorResponse of(ErrorCode code, String path) {
        return of(code, code.defaultMessage(), path, List.of());
    }

    public static ApiErrorResponse of(ErrorCode code, String message, String path) {
        return of(code, message, path, List.of());
    }

    public static ApiErrorResponse of(
            ErrorCode code,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors
    ) {
        return new ApiErrorResponse(
                code.name(),
                message,
                Instant.now(),
                path,
                fieldErrors == null ? List.of() : fieldErrors
        );
    }
}
