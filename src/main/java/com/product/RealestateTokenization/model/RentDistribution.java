package com.product.RealestateTokenization.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class RentDistribution {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long propertyId;
    private LocalDate distributionDate;
    private double totalRentDistributed;
    private String status;  // PENDING, COMPLETED, FAILED
    private String razorpayBatchId;  // Optional for batch tracking

    @OneToMany(mappedBy = "rentDistribution", cascade = CascadeType.ALL)
    private List<Transaction> payouts = new ArrayList<>();  // Reuse Transaction for individual payouts
}