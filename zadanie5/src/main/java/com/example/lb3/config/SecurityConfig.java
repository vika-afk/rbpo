package com.example.lb3.config;

import com.example.lb3.security.JwtAuthenticationFilter;
import com.example.lb3.security.RestAccessDeniedHandler;
import com.example.lb3.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final RequestMatcher BEARER_REQUEST = request -> {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null
                && header.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
    };

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(BEARER_REQUEST)
                        .ignoringAntMatchers("/api/auth/login", "/api/auth/refresh")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeRequests(auth -> auth
                        .antMatchers("/api/auth/csrf", "/api/auth/register",
                                "/api/auth/login", "/api/auth/refresh").permitAll()
                        .antMatchers("/api/auth/me").authenticated()
                        .antMatchers(HttpMethod.GET, "/api/plays/**", "/api/theatre-halls/**",
                                "/api/performances/**").permitAll()
                        .antMatchers("/api/users/**").hasRole("ADMIN")
                        .antMatchers(HttpMethod.POST, "/api/tickets/purchase", "/api/tickets/*/refund")
                        .authenticated()
                        .antMatchers(HttpMethod.POST, "/api/tickets/**").hasAnyRole("ADMIN", "MANAGER")
                        .antMatchers(HttpMethod.PATCH, "/api/tickets/**").hasAnyRole("ADMIN", "MANAGER")
                        .antMatchers(HttpMethod.DELETE, "/api/tickets/**").hasRole("ADMIN")
                        .antMatchers(HttpMethod.POST, "/api/plays/**", "/api/performances/**",
                                "/api/theatre-halls/**").hasAnyRole("ADMIN", "MANAGER")
                        .antMatchers(HttpMethod.PATCH, "/api/plays/**", "/api/performances/**",
                                "/api/theatre-halls/**").hasAnyRole("ADMIN", "MANAGER")
                        .antMatchers(HttpMethod.DELETE, "/api/plays/**", "/api/performances/**",
                                "/api/theatre-halls/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
