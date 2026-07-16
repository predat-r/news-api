package com.training.news.security;


import com.training.news.security.token.DatabaseOpaqueTokenIntrospector;
import com.training.news.security.token.TokenAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            TokenAuthenticationSuccessHandler tokenAuthenticationSuccessHandler,
                                            DatabaseOpaqueTokenIntrospector databaseOpaqueTokenIntrospector) throws Exception {

        http.csrf(csrf -> csrf
                        .disable()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/news", "/api/v1/news/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(formLogin -> formLogin.successHandler(tokenAuthenticationSuccessHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer.opaqueToken(opaqueToken -> opaqueToken.introspector(databaseOpaqueTokenIntrospector)));
        return http.build();
    }


}
