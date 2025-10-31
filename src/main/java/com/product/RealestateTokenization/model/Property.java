package com.product.RealestateTokenization.model;


import jakarta.persistence.*;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Property {
    @Id @GeneratedValue private Long id;
    private String name, location, tokenSymbol, status = "CREATED", spvId;
    private double propertyValue, tokenPrice;
    private int totalTokens, availableTokens;
    @Column(columnDefinition = "TEXT")
    private String unsignedTxBlob; // Stores unsigned JSON
    public void setSpvId(String spvId) { this.spvId = spvId; }
    public void setTokenPrice(double price) { this.tokenPrice = price; }
    public double getTokenPrice() { return tokenPrice; }


}