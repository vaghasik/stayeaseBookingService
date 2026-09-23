package com.apexon.stayease.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder should return BCryptPasswordEncoder")
    void testPasswordEncoder() {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        CorsConfigurationSource cors = mock(CorsConfigurationSource.class);

        SecurityConfig cfg = new SecurityConfig(userDetailsService, jwtFilter, cors);
        PasswordEncoder encoder = cfg.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("authenticationProvider should be configured with a password encoder")
    void testAuthenticationProvider() {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        CorsConfigurationSource cors = mock(CorsConfigurationSource.class);

        SecurityConfig cfg = new SecurityConfig(userDetailsService, jwtFilter, cors);
        var provider = cfg.authenticationProvider();

        assertThat(provider).isNotNull();
        // Inspect passwordEncoder via reflection to avoid relying on provider getters
        try {
            java.lang.reflect.Field peField =
                    org.springframework.security.authentication.dao.DaoAuthenticationProvider.class
                            .getDeclaredField("passwordEncoder");
            peField.setAccessible(true);
            Object pe = peField.get(provider);
            assertThat(pe).isNotNull();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("authenticationManager delegates to AuthenticationConfiguration")
    void testAuthenticationManagerDelegates() throws Exception {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        CorsConfigurationSource cors = mock(CorsConfigurationSource.class);

        SecurityConfig cfg = new SecurityConfig(userDetailsService, jwtFilter, cors);

        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager am = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(am);

        AuthenticationManager result = cfg.authenticationManager(authConfig);
        assertThat(result).isEqualTo(am);
        verify(authConfig).getAuthenticationManager();
    }

    @Test
    @DisplayName("filterChain should configure HttpSecurity and add jwt filter")
    void testFilterChainConfiguresHttpSecurity() throws Exception {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        CorsConfigurationSource cors = mock(CorsConfigurationSource.class);

        SecurityConfig cfg = spy(new SecurityConfig(userDetailsService, jwtFilter, cors));

        // Mock HttpSecurity and fluent methods to return the same mock for chaining
        HttpSecurity http = mock(HttpSecurity.class);
        when(http.csrf(any())).thenReturn(http);
        when(http.cors(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.authenticationProvider(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any(Class.class))).thenReturn(http);
        when(http.headers(any())).thenReturn(http);

        org.springframework.security.web.DefaultSecurityFilterChain chain =
                mock(org.springframework.security.web.DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        // Call filterChain
        SecurityFilterChain result = cfg.filterChain(http);

        assertThat(result).isSameAs(chain);
        verify(http).addFilterBefore(eq(jwtFilter), eq(UsernamePasswordAuthenticationFilter.class));
        verify(http).authenticationProvider(any());
    }

    @Test
    @DisplayName("filterChain should apply csrf, cors, session, headers and authorization config")
    void testFilterChainConfiguresAllOptions() throws Exception {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        CorsConfigurationSource corsSource = mock(CorsConfigurationSource.class);

        SecurityConfig cfg = new SecurityConfig(userDetailsService, jwtFilter, corsSource);

        HttpSecurity http = mock(HttpSecurity.class);
        when(http.csrf(any())).thenReturn(http);
        when(http.cors(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.authenticationProvider(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any(Class.class))).thenReturn(http);
        when(http.headers(any())).thenReturn(http);

        org.springframework.security.web.DefaultSecurityFilterChain chain =
                mock(org.springframework.security.web.DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        // Call filterChain
        SecurityFilterChain result = cfg.filterChain(http);

        assertThat(result).isSameAs(chain);

        // Verify top-level http builder steps were invoked
        verify(http).csrf(any());
        verify(http).cors(any());
        verify(http).sessionManagement(any());
        verify(http).authorizeHttpRequests(any());
        verify(http).headers(any());

        // Verify authentication provider and jwt filter were registered
        verify(http).authenticationProvider(any());
        verify(http).addFilterBefore(eq(jwtFilter), eq(UsernamePasswordAuthenticationFilter.class));
    }

    @Test
    @DisplayName("Application context builds SecurityFilterChain bean and registers jwt filter")
    void testFilterChainBuiltInApplicationContext() {
        try (AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(TestAppConfig.class, SecurityConfig.class)) {
            SecurityFilterChain chain = ctx.getBean(SecurityFilterChain.class);
            JwtAuthenticationFilter jwt = ctx.getBean(JwtAuthenticationFilter.class);

            assertThat(chain).isInstanceOf(DefaultSecurityFilterChain.class);
            DefaultSecurityFilterChain dchain = (DefaultSecurityFilterChain) chain;
            assertThat(dchain.getFilters()).contains(jwt);
        }
    }

    @Configuration
    static class TestAppConfig {
        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            return mock(CorsConfigurationSource.class);
        }
    }
}
