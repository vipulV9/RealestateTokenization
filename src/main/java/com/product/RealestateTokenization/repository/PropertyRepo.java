package com.product.RealestateTokenization.repository;


import com.product.RealestateTokenization.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PropertyRepo extends JpaRepository<Property, Long> {
    List<Property> findByStatusIn(List<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :id")
    Optional<Property> findByIdForUpdate(Long id);
}