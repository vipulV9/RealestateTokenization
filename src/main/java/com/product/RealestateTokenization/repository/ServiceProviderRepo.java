package com.product.RealestateTokenization.repository;

import com.product.RealestateTokenization.model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceProviderRepo extends JpaRepository<ServiceProvider, Long> {

    Optional<ServiceProvider> findByName(String name);

}
