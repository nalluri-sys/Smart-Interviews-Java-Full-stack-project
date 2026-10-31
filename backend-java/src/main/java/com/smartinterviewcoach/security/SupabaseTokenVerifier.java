package com.smartinterviewcoach.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URL;
import java.util.Date;

@Component
@Slf4j
public class SupabaseTokenVerifier {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    @PostConstruct
    public void init() {
        try {
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(
                new URL(supabaseUrl + "/auth/v1/.well-known/jwks.json"));
            JWSVerificationKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(keySelector);
        } catch (Exception e) {
            log.error("Failed to initialize Supabase token verifier", e);
            throw new IllegalStateException("Cannot initialize Supabase JWT verification", e);
        }
    }

    public SupabaseClaims verify(String accessToken) {
        try {
            JWTClaimsSet claims = jwtProcessor.process(accessToken, null);
            String expectedIssuer = supabaseUrl + "/auth/v1";
            if (!expectedIssuer.equals(claims.getIssuer())) {
                log.warn("Invalid Supabase token issuer: {}", claims.getIssuer());
                return null;
            }
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                log.warn("Supabase token is expired");
                return null;
            }
            if (!claims.getAudience().isEmpty() && !claims.getAudience().contains("authenticated")) {
                log.warn("Invalid Supabase token audience");
                return null;
            }

            return new SupabaseClaims(
                claims.getSubject(),
                (String) claims.getClaim("email"),
                (String) claims.getClaim("user_name"),
                (String) claims.getClaim("avatar_url")
            );
        } catch (Exception e) {
            log.warn("Supabase token verification failed: {}", e.getMessage());
            return null;
        }
    }

    public record SupabaseClaims(String uid, String email, String displayName, String photoUrl) {}
}
