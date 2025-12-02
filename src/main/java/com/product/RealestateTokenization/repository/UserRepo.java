package com.product.RealestateTokenization.repository;


import com.product.RealestateTokenization.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // Add this custom query
    @Query("SELECT u FROM User u WHERE u.phone LIKE %:last10Digits")
    Optional<User> findByPhoneEndingWith(String last10Digits);

    // Alternative (cleaner): store phone in normalized form from beginning!
    Optional<User> findByPhone(String phone); // if you store +919876543210
}