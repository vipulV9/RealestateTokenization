package com.product.RealestateTokenization.Elasticsearch;

import com.product.RealestateTokenization.model.Property;

public class PropertyMapper {

    public static PropertySearch toSearch(Property p) {

        PropertySearch s = new PropertySearch();

        s.setId(String.valueOf(p.getId()));
        s.setName(p.getName());
        s.setAbout(p.getAbout());
        s.setLocationTitle(p.getLocationTitle());

        s.setPropertyValue(p.getPropertyValue());
        s.setTokenPrice(p.getTokenPrice());
        s.setMonthlyRent(p.getMonthlyRent());

        s.setType(p.getType() != null ? p.getType().name() : null);
        s.setState(p.getState() != null ? p.getState().name() : null);
        s.setFurnished(p.getFurnished() != null ? p.getFurnished().name() : null);

        s.setIslive(p.isIslive());
        s.setApproved(p.getIsApproved());
        if (p.getCoordinates() != null) {
            s.setLat(p.getCoordinates().getLat());
            s.setLng(p.getCoordinates().getLng());
        }

        if (p.getRooms() != null) {
            s.setRoomTypes(
                    p.getRooms().stream()
                            .map(r -> r.getType())
                            .toList()
            );
        }

        s.setPhotoUrls(p.getPhotoUrls());

        return s;
    }
}