package com.futuremessage.domain;

/**
 * Quyền tài khoản. Đăng ký công khai luôn tạo {@code USER}.
 * {@code ADMIN} chỉ được seed từ env / bootstrap, không tự nâng trên UI.
 */
public enum UserRole {
    USER,
    ADMIN
}
