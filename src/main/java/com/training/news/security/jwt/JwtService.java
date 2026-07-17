package com.training.news.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder jwtEncoder;


    String issueToken(Authentication authentication) {
        Instant currentTime = Instant.now();
        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(role -> role.getAuthority())
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtTokenSettings.ISSUER)
                .subject(authentication.getName())
                .issuedAt(currentTime)
                .expiresAt(currentTime.plus(JwtTokenSettings.ACCESS_TOKEN_LIFETIME))
                .id(UUID.randomUUID()
                        .toString())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}
