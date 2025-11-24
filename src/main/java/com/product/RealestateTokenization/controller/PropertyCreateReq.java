package com.product.RealestateTokenization.controller;

import java.util.List;

public record PropertyCreateReq
        (String name,
         double lat,
         double lng,
         double value,
         int totalTokens,
         double tokenPrice,
         String spvId,
         String tokenSymbol,
         List<RoomReq> rooms
        )
        {
                public static record RoomReq(
                        String type,
                        int value
                ) {}
        }
