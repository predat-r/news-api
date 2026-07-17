package com.training.news.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {


    @Bean
    KeyPair jwtKeyPair(@Value("${spring.security.jwt.private-key}") String privateKeyValue,
                       @Value("${spring.security.jwt.public-key}") String publicKeyValue

    ) throws GeneralSecurityException {

        byte[] privateKeyBytes = Base64.getDecoder()
                .decode(privateKeyValue);

        byte[] publicKeyBytes = Base64.getDecoder()
                .decode(publicKeyValue);

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair keyPair) {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        JwtEncoder jwtEncoder = NimbusJwtEncoder.withKeyPair(rsaPublicKey, rsaPrivateKey)
                .build();
        return jwtEncoder;

    }

    @Bean
    JwtDecoder jwtDecoder(KeyPair keyPair) {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey)
                .build();

        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtTokenSettings.ISSUER));

        return jwtDecoder;
    }
}
