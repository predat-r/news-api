package com.training.news.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiUserRepository extends JpaRepository<ApiUser,Long> {
    Optional<ApiUser> findByUsername(String username);
    Optional<ApiUser> findByToken(String token);
}
