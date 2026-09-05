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
}
