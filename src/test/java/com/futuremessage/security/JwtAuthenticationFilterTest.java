package com.futuremessage.security;

import com.futuremessage.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    @Test
    void userHasOnlyUserRole() {
        assertThat(JwtAuthenticationFilter.authoritiesFor(UserRole.USER))
                .containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void adminKeepsUserRoleAndGainsAdmin() {
        assertThat(JwtAuthenticationFilter.authoritiesFor(UserRole.ADMIN))
                .containsExactly(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                );
    }
}
