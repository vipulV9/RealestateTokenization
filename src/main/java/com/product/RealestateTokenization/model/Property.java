package com.product.RealestateTokenization.model;


import jakarta.persistence.*;


import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data

public class Property {
    @Id @GeneratedValue private Long id;
    private String name , tokenSymbol, status = "CREATED", spvId;
    private double propertyValue, tokenPrice;
    private double currentValue;
    private int totalTokens, availableTokens;
    @Column(columnDefinition = "TEXT")
    private String unsignedTxBlob;
    @Embedded
    private Coordinates coordinates;
    @Embedded
    private Room room;

    @ElementCollection
    @CollectionTable(name = "property_rooms", joinColumns = @JoinColumn(name = "property_id"))
    private List<Room> rooms = new ArrayList<>();

    @Embeddable
    @Data
    public static class Room {
        private int value;
        private String type;
    }

    @Embeddable
    @Data
    public static class Coordinates {
        private double lat;
        private double lng;
    }




    @ElementCollection
    @CollectionTable(name = "property_photos", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "photo_url")
    private List<String> photoUrls = new ArrayList<>();

    public void setSpvId(String spvId) { this.spvId = spvId; }
    public void setTokenPrice(double price) { this.tokenPrice = price; }
    public double getTokenPrice() { return tokenPrice; }
}
