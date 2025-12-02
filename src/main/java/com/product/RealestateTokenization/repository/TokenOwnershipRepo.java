package com.product.RealestateTokenization.repository;

import com.product.RealestateTokenization.model.TokenOwnership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenOwnershipRepo extends JpaRepository<TokenOwnership, Long> {
    Optional<TokenOwnership> findByUserIdAndPropertyId(Long userId, Long propertyId);
    List<TokenOwnership> findByPropertyId(Long propertyId);
}
