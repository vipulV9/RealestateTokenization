package com.product.RealestateTokenization.model;


import java.util.List;

public record NormalPropertyCreateReq(
        String name,
        String about,
        double lat,
        double lng,
        double monthlyRent,
        double security,
        double size,
        String locationTitle,
        PropertyType type,
        Furnished furnished,
        PropertyState state,
        List<RoomReq> rooms,
        List<String> services
) {

    public static record RoomReq(
            String type,
            int value
    ) {}
}
