package com.apexon.stayease.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.apexon.stayease.entity.User;
import com.apexon.stayease.enums.UserRole;

class CustomUserDetailsTest {

    @Test
    @DisplayName("getPassword returns the password field when constructed directly")
    void testGetPasswordDirect() {
        UUID id = UUID.randomUUID();
        String email = "direct@example.com";
        String pwd = "direct-secret";
        String role = "MANAGER";

        CustomUserDetails details = new CustomUserDetails(id, email, pwd, role, true);

        assertThat(details.getPassword()).isEqualTo(pwd);
        assertThat(details.getUsername()).isEqualTo(email);
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("create(User) maps fields correctly including passwordHash")
    void testCreateFromUser() {
        User user =
                User.builder()
                        .userId(UUID.randomUUID())
                        .emailAddress("user@example.com")
                        .fullName("User Example")
                        .passwordHash("hashed-pwd")
                        .userRole(UserRole.MANAGER)
                        .isActive(false)
                        .build();

        CustomUserDetails details = CustomUserDetails.create(user);

        assertThat(details.getUserId()).isEqualTo(user.getUserId());
        assertThat(details.getUsername()).isEqualTo(user.getEmailAddress());
        assertThat(details.getPassword()).isEqualTo(user.getPasswordHash());
        assertThat(details.getAuthorities()).isNotEmpty();
        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo(user.getUserRole().name());
        assertThat(details.isEnabled()).isFalse();
    }
}
