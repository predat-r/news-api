package com.training.news.security.jwt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtResponseWriter {
    private final ObjectMapper objectMapper;


    public void writeResponse(HttpServletResponse response, String token) throws IOException {
        JwtResponse jwtResponse = new JwtResponse(token, "Bearer", JwtTokenSettings.ACCESS_TOKEN_LIFETIME.toSeconds());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), jwtResponse);
    }
}
