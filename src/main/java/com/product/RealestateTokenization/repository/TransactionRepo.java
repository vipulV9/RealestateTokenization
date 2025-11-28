package com.product.RealestateTokenization.repository;

import com.product.RealestateTokenization.model.Transaction;
import com.product.RealestateTokenization.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {

    boolean existsByIdempotencyKey(String key);

    Optional<Transaction> findByRazorpayPaymentId(String id);

//    List<Transaction> findByStatus(String status);
     List<Transaction> findByStatus(TransactionStatus status);

    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);

    // ✅ Correct usage for pessimistic locking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);

    List<Transaction> findByUserIdAndStatus(Long userId, TransactionStatus status);

    List<Transaction> findByUserId(Long userId);
}
