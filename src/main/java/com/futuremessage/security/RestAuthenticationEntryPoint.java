package com.futuremessage.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuremessage.common.ApiErrorResponse;
import com.futuremessage.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ApiErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI()));
    }

    public void writeInvalidToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ApiErrorResponse.of(ErrorCode.INVALID_TOKEN, request.getRequestURI()));
    }

    public void writeRateLimited(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        write(response, HttpStatus.TOO_MANY_REQUESTS, ApiErrorResponse.of(ErrorCode.RATE_LIMITED, request.getRequestURI()));
    }

    private void write(HttpServletResponse response, HttpStatus status, ApiErrorResponse body) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
