package com.product.RealestateTokenization.Elasticsearch;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

@Data
@Document(indexName = "properties")
public class PropertySearch {

    @Id
    private String id;

    private String name;
    private String about;
    private String locationTitle;

    private double propertyValue;
    private double tokenPrice;
    private double monthlyRent;

    private String type;
    private String state;
    private String furnished;

    private boolean isApproved;
    private boolean islive;

    private double lat;
    private double lng;

    private List<String> roomTypes;

    private List<String> photoUrls;
}

