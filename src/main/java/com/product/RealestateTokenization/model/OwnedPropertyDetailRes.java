package com.product.RealestateTokenization.model;

public record OwnedPropertyDetailRes(
        Long propertyId,
        String name,
        int totalTokens,
        double tokenPrice,
        double currentValue,
        int userTokens,
        double userInvestment
) {}
