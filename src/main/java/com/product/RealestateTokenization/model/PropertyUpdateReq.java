package com.product.RealestateTokenization.model;

public record PropertyUpdateReq(
        String name,
        Double currentValue,
        Double propertyValue,
        Integer totalTokens,
        Double tokenPrice,
        String spvId,
        String tokenSymbol,
        Double monthlyRent
) {}
