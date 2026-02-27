package com.product.RealestateTokenization.repository;

import com.product.RealestateTokenization.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryRepo extends JpaRepository<Enquiry, Long> {
}