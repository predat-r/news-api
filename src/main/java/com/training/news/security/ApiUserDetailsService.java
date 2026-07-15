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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ApiUser user = apiUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User with this username does not exist"));
        return User.builder().username(user.getUsername()).password(user.getPassword()).roles(user.getRole().name()).build();
    }
}
