package com.product.RealestateTokenization.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.product.RealestateTokenization.controller.PropertyCreateReq;
import com.product.RealestateTokenization.model.Property;
import com.product.RealestateTokenization.model.PropertyUpdateReq;
import com.product.RealestateTokenization.repository.PropertyRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PropertyService {

    private final PropertyRepo repo;
    private final XrplService xrplService;

    @Autowired
    private Cloudinary cloudinary;

    public PropertyService(PropertyRepo repo, XrplService xrplService) {
        this.repo = repo;
        this.xrplService = xrplService;
    }

//    @Transactional
//    public Property createPropertyWithUnsignedTx(PropertyCreateReq req) throws Exception {
//        Property p = new Property();
//        p.setName(req.name());
//        p.setLocation(req.location());
//        p.setPropertyValue(req.value());
//        p.setTotalTokens(req.totalTokens());
//        p.setTokenPrice(req.tokenPrice());
//        p.setSpvId(req.spvId());
//        p.setStatus("CREATED");
//        p.setTokenSymbol("PRP");
//        p.setAvailableTokens(0); // Will be set after issuance
//
//        // Generate unsigned issuance transaction
//        String unsignedJson = xrplService.generateUnsignedIssuance(p.getTokenSymbol(), req.totalTokens());
//        p.setUnsignedTxBlob(unsignedJson);
//
//        return repo.save(p);
//    }
//@Transactional
//public Property createPropertyWithUnsignedTx(PropertyCreateReq req, List<MultipartFile> images) throws Exception {
//    Property p = new Property();
//    p.setName(req.name());
//    p.setLocation(req.location());
//    p.setPropertyValue(req.value());
//    p.setTotalTokens(req.totalTokens());
//    p.setTokenPrice(req.tokenPrice());
//    p.setSpvId(req.spvId());
//    p.setStatus("CREATED");
//    p.setTokenSymbol("PRP");
//    p.setAvailableTokens(0);
//
//    // ✅ Cloudinary upload logic
//    List<String> uploadedUrls = new ArrayList<>();
//    for (MultipartFile image : images) {
//        Map uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.emptyMap());
//        uploadedUrls.add((String) uploadResult.get("url"));
//    }
//    p.setPhotoUrls(uploadedUrls);
//
//    // Generate unsigned issuance transaction
//    String unsignedJson = xrplService.generateUnsignedIssuance(p.getTokenSymbol(), req.totalTokens());
//    p.setUnsignedTxBlob(unsignedJson);
//
//    return repo.save(p);
//}

    @Transactional
    public Property createPropertyWithUnsignedTx(PropertyCreateReq req, List<MultipartFile> images) throws Exception {
        Property p = new Property();
        p.setName(req.name());

        Property.Coordinates coords = new Property.Coordinates();
        coords.setLat(req.lat());
        coords.setLng(req.lng());
        p.setCoordinates(coords);

        p.setPropertyValue(req.value());
        p.setCurrentValue(req.value());
        p.setTotalTokens(req.totalTokens());
        p.setTokenPrice(req.tokenPrice());
        p.setSpvId(req.spvId());
        p.setStatus("CREATED");

        // ------------ ADD ROOMS (not hardcoded) ----------
        if (req.rooms() != null && !req.rooms().isEmpty()) {
            List<Property.Room> roomList = new ArrayList<>();

            for (PropertyCreateReq.RoomReq r : req.rooms()) {
                Property.Room room = new Property.Room();
                room.setType(r.type());
                room.setValue(r.value());
                roomList.add(room);
            }

            p.setRooms(roomList);
        }
        // Validate and set token symbol (must be 3 uppercase letters)
        String tokenSymbol = req.tokenSymbol();
        if (tokenSymbol == null || !tokenSymbol.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Token symbol must be exactly 3 uppercase letters (e.g., PRP, SKY, GVA)");
        }
        p.setTokenSymbol(tokenSymbol);

        // Initially no tokens distributed
        p.setAvailableTokens(0);

        // Cloudinary upload
        List<String> uploadedUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                Map uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.emptyMap());
                uploadedUrls.add((String) uploadResult.get("url"));
            }
        }
        p.setPhotoUrls(uploadedUrls);

        // Generate unsigned issuance
        String unsignedJson = xrplService.generateUnsignedIssuance(p.getTokenSymbol(), req.totalTokens());
        p.setUnsignedTxBlob(unsignedJson);

        // Save first so we have the entity
        p = repo.save(p);

        // NOW enable rippling from Cold → Hot for this token
        // This runs ONCE per property, safely, and fixes tecPATH_DRY forever
        xrplService.ensureIssuerAllowsRipplingToHot(p.getTokenSymbol());

        return p;
    }


    public List<Property> getOnChainProperties() {
        return repo.findByStatusIn(List.of("ON_CHAIN", "HOT_READY"));
    }

    public Property getProperty(Long id) {
        return repo.findById(id).orElseThrow();
    }

//    @Transactional
//    public Property broadcastSignedIssuance(Long id, String signedTxBlob) throws Exception {
//        Property p = repo.findByIdForUpdate(id).orElseThrow();
//        if (!"CREATED".equals(p.getStatus())) {
//            throw new IllegalStateException("Property not in CREATED state");
//        }
//
//        String hash = xrplService.broadcastSigned(signedTxBlob);
//        p.setStatus("ON_CHAIN");
//        p.setAvailableTokens(p.getTotalTokens());
//        p.setUnsignedTxBlob(null); // Clear after broadcast
//
//        return repo.save(p);
//    }
@Transactional
public Property broadcastSignedIssuance(Long id, String signedTxBlob) throws Exception {
    Property p = repo.findByIdForUpdate(id).orElseThrow();
    if (!"CREATED".equals(p.getStatus())) {
        throw new IllegalStateException("Property not in CREATED state");
    }

    String transactionHash = xrplService.broadcastSigned(signedTxBlob);
    p.setStatus("ON_CHAIN");
    p.setAvailableTokens(p.getTotalTokens());
    p.setUnsignedTxBlob(null);

    // You might want to save transactionHash somewhere in property for reference

    return repo.save(p);
}

    @Transactional
    public Property updateProperty(Long id, PropertyUpdateReq req) {
        Property p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Update name
        if (req.name() != null)
            p.setName(req.name());

        // Update currentValue → auto adjust tokenPrice
        if (req.currentValue() != null) {
            p.setCurrentValue(req.currentValue());

            // Adjust token price based on new market/current value
            double newTokenPrice = req.currentValue() / p.getTotalTokens();
            p.setTokenPrice(newTokenPrice);
        }

        // Update propertyValue
        if (req.propertyValue() != null)
            p.setPropertyValue(req.propertyValue());

        // Update total tokens
        if (req.totalTokens() != null) {
            p.setTotalTokens(req.totalTokens());

            // Re-adjust token price after totalTokens changes
            double adjustedPrice = p.getCurrentValue() / req.totalTokens();
            p.setTokenPrice(adjustedPrice);
        }

        // Update SPV id
        if (req.spvId() != null)
            p.setSpvId(req.spvId());

        // Update token symbol
        if (req.tokenSymbol() != null)
            p.setTokenSymbol(req.tokenSymbol());

        return repo.save(p);
    }



}
