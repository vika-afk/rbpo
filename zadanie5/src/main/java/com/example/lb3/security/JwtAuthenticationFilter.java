package com.example.lb3.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/api/auth/login".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/auth/csrf".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.validateAccessToken(header.substring(BEARER_PREFIX.length()).trim());
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!userDetails.isEnabled()) {
                throw new DisabledException("Учётная запись недоступна");
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("Недействительный access-токен", ex));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
