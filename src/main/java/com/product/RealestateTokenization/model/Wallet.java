package com.product.RealestateTokenization.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Wallet {
    @Id @GeneratedValue private Long id;
    private Long userId;
    private String address, secretEncrypted;
    private boolean fundedXrp = false;

    public void setUser(User user) { this.userId = user.getId(); }
}