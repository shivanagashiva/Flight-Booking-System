package com.flightbooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.flightbooking.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	
    Optional<User> findById(Long userId);

    User findByUsername(String username);

    boolean existsByUsername(String username);
}
