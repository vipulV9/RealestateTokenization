package com.product.RealestateTokenization.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class SpvCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String cin;
    private String registeredAddress;
    private String email;

    // Signing authority
    private String directorName;
    private String directorDin;
    private String signingPlace;

    // Bank details (issuer bank)
    private String bankAccountNumber;
    private String bankIfsc;
    private String beneficiaryName;
}

