package com.product.RealestateTokenization.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@IdClass(TokenOwnershipId.class)
public class TokenOwnership {
    @Id private Long userId;
    @Id private Long propertyId;
    private int tokens;
}

@Data
class TokenOwnershipId implements java.io.Serializable {
    private Long userId;
    private Long propertyId;
}