package com.product.RealestateTokenization.model;

import java.util.List;

public record UserInvestmentDTO(
        Long userId,
        String name,
        String email,
        String phone,
        String kycStatus,
        String custodyMode,
        double totalInvestedAmount,
        double currentPortfolioValue,
        List<PropertyHolding> holdings
) {
    public record PropertyHolding(
            Long propertyId,
            String propertyName,
            List<String> photoUrls ,
            String tokenSymbol,
            int tokensOwned,
            double tokenPrice,
            double investedAmount,
            double currentValue
    ) {}
}

