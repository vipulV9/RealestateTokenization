package com.product.RealestateTokenization.model;

public record UserProfileDTO(
        Long id,
        String name,
        String email,
        String phone,
        String kycStatus,
        String custodyMode
) {}
