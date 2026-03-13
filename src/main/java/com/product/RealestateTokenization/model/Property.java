package com.product.RealestateTokenization.model;


import jakarta.persistence.*;


import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data

public class Property {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name,about, tokenSymbol, status = "CREATED", spvId;
    private double propertyValue, tokenPrice;
    private double currentValue;
    private double security;
    private double Size;
    private int totalTokens, availableTokens;
    @Column(columnDefinition = "TEXT")
    private String unsignedTxBlob;
    @Embedded
    private Coordinates coordinates;
    private double monthlyRent = 0.0;

    private Boolean isApproved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    @ManyToOne
    @JoinColumn(name = "spv_company_id")
    private SpvCompany spvCompany;

    @Enumerated(EnumType.STRING)
    private PropertyType type;

    @Enumerated(EnumType.STRING)
    private PropertyState state;

    private boolean islive;

    @Enumerated(EnumType.STRING)
    private Furnished furnished;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "property_services",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<ServiceProvider> services = new ArrayList<>();

//    @Embedded
//    private Room room;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_rooms", joinColumns = @JoinColumn(name = "property_id"))
    private List<Room> rooms = new ArrayList<>();

    @Embeddable
    @Data
    public static class Room {
        private int value;
        private String type;
    }
    private Double annualRoiPercentage;

    String locationTitle;
    @Embeddable
    @Data
    public static class Coordinates {
        private double lat;
        private double lng;
    }



    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_photos", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "photo_url")
    private List<String> photoUrls = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_videos", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "video_url")
    private List<String> videoUrls = new ArrayList<>();


    public void setTokenPrice(double price) { this.tokenPrice = price; }
    public double getTokenPrice() { return tokenPrice; }
    @Embeddable
    @Data
    public static class OwnerDetails {
        private String name;
        private String phone;
        private String email;
    }
}
