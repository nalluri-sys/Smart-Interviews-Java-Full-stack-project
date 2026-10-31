package com.smartinterviewcoach.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseAuthFilter extends OncePerRequestFilter {

    private final SupabaseTokenVerifier tokenVerifier;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            SupabaseTokenVerifier.SupabaseClaims claims =
                tokenVerifier.verify(authHeader.substring(7));
            if (claims != null) {
                AuthenticatedUser principal = new AuthenticatedUser(
                    claims.uid(), claims.email(), claims.displayName(), claims.photoUrl());
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
                log.debug("Authenticated Supabase user: uid={}", claims.uid());
            } else {
                log.warn("Invalid Supabase token on request to: {}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/health") || path.startsWith("/actuator");
    }
}
