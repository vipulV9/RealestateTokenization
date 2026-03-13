package com.product.RealestateTokenization.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;        // Airtel, Jio, Blinkit

    @Enumerated(EnumType.STRING)
    private ServiceCategory category;

}
