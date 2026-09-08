package com.futuremessage.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void setEmailNormalizesToLowercaseTrimmed() {
        User user = new User();
        user.setEmail("  Ada@Example.COM ");

        assertThat(user.getEmail()).isEqualTo("ada@example.com");
        assertThat(user.hasEmail("ADA@example.com")).isTrue();
    }

    @Test
    void builderDefaultsToUserRoleAndEnabled() {
        User user = User.builder()
                .email("ada@example.com")
                .passwordHash("hashed")
                .displayName("Ada")
                .build();

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAdmin()).isFalse();
        assertThat(user.roleOrDefault()).isEqualTo(UserRole.USER);
    }

    @Test
    void adminFlagFollowsRole() {
        User user = User.builder().role(UserRole.ADMIN).build();

        assertThat(user.isAdmin()).isTrue();
    }
}
