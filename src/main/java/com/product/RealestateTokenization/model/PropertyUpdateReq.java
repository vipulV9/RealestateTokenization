package com.product.RealestateTokenization.model;

public record PropertyUpdateReq(
        String name,
        Double currentValue,
        Double propertyValue,
        Integer totalTokens,
        Double tokenPrice,
        String spvId,
        String tokenSymbol,
        Double monthlyRent,
        String spvBankAccountNumber,
        String spvBankIfsc,
        String spvBeneficiaryName,
        String pas4DocumentUrl,
        Double annualRoiPercentage
) {}
