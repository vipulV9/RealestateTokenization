package com.product.RealestateTokenization.model;

import java.util.List;

public record PropertyCreateReq
        (String name,
         double lat,
         double lng,
         double value,
         String about,
         double currentValue,
         double security,
         int totalTokens,
         int availableTokens,
         double tokenPrice,
         String tokenSymbol,
         Double annualRoiPercentage,
         double monthlyRent,
         PropertyType type,
         Furnished furnishl,
         PropertyState state,
         List<RoomReq> rooms,
         SpvCompanyReq spvCompany
        )
        {
                public static record RoomReq(
                        String type,
                        int value
                ) {}
                public record SpvCompanyReq(
                        String name,
                        String cin,
                        String registeredAddress,
                        String email,
                        String directorName,
                        String directorDin,
                        String signingPlace,
                        String bankAccountNumber,
                        String bankIfsc,
                        String beneficiaryName
                ) {}
        }
