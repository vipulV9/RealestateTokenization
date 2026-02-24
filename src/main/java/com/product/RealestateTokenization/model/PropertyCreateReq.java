package com.product.RealestateTokenization.model;

import org.springframework.beans.PropertyValue;

import java.util.List;

public record PropertyCreateReq
        (String name,
         double lat,
         double lng,
         double value,
         String about,
         double propertyValue,
         double currentValue,
         double security,
         double Size,
         int totalTokens,
         int availableTokens,
         double tokenPrice,
         String tokenSymbol,
         Double annualRoiPercentage,
         double monthlyRent,
         String ownername,
         double ownerphno,
         PropertyType type,
         Furnished furnished,
         PropertyState state,
         boolean islive,
         String locationTitle,
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
