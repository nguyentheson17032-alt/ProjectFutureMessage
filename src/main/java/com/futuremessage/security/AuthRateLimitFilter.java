package com.futuremessage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    );

    private final InMemoryRateLimiter authRateLimiter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final Clock clock;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr();
        if (!authRateLimiter.tryAcquire(key, clock.instant())) {
            authenticationEntryPoint.writeRateLimited(
                    request,
                    response,
                    authRateLimiter.window().toSeconds()
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isLimited(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }
        return LIMITED_PATHS.contains(request.getServletPath());
    }
}
