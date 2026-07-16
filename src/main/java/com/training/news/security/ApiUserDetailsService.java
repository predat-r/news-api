package com.training.news.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiUserDetailsService implements UserDetailsService {
    private final ApiUserRepository apiUserRepository;

    // TODO: Add end-to-end tests for login, CSRF, role permissions, and reporter ownership.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ApiUser user = apiUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
        return User.builder().username(user.getUsername()).password(user.getPassword()).roles(user.getRole().name()).build();
    }
}
