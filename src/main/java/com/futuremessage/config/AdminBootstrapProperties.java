package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap tài khoản ADMIN lúc startup. Không cấu hình email/password thì bỏ qua.
 * Local có mặc định trong {@code application.yml}; prod chỉ tạo khi set env.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminBootstrapProperties(
        String email,
        String password,
        String displayName
) {

    public AdminBootstrapProperties {
        email = email == null ? "" : email.trim().toLowerCase();
        password = password == null ? "" : password;
        displayName = displayName == null || displayName.isBlank() ? "Admin" : displayName.trim();
        boolean configured = !email.isBlank() && !password.isBlank();
        if (configured && password.length() < 8) {
            throw new IllegalStateException("app.admin.password must be at least 8 characters");
        }
        if (configured && password.length() > 72) {
            throw new IllegalStateException("app.admin.password must be at most 72 characters");
        }
        if (configured && !email.contains("@")) {
            throw new IllegalStateException("app.admin.email must be a valid email");
        }
    }

    public boolean configured() {
        return !email.isBlank() && !password.isBlank();
    }
}
