package com.product.RealestateTokenization.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.PropertyRepo;
import com.product.RealestateTokenization.repository.SpvCompanyRepo;
import com.product.RealestateTokenization.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class PropertyService {

    private final PropertyRepo repo;
    private final XrplService xrplService;
    private final SpvCompanyRepo spvCompanyRepo;
    private final UserRepo userRepo;


    @Autowired
    private Cloudinary cloudinary;

    public PropertyService(PropertyRepo repo, XrplService xrplService, SpvCompanyRepo spvCompanyRepo, UserRepo userRepo) {
        this.repo = repo;
        this.xrplService = xrplService;
        this.spvCompanyRepo = spvCompanyRepo;
        this.userRepo = userRepo;
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
    public Property createPropertyWithUnsignedTx(PropertyCreateReq req, List<MultipartFile> images,    List<MultipartFile> videos
    ) throws Exception {
        Property p = new Property();
        p.setName(req.name());
        p.setAbout(req.about());
        Property.Coordinates coords = new Property.Coordinates();
        coords.setLat(req.lat());
        coords.setLng(req.lng());
        p.setCoordinates(coords);
        p.setPropertyValue(req.value());
        p.setCurrentValue(req.value());
        p.setTotalTokens(req.totalTokens());
        p.setAvailableTokens(req.availableTokens());
        p.setTokenPrice(req.tokenPrice());
        p.setMonthlyRent(req.monthlyRent());
        p.setAnnualRoiPercentage(req.annualRoiPercentage());
        p.setSecurity(req.security());
        p.setSize(req.Size());
        p.setLocationTitle(req.locationTitle());
        p.setState(req.state());
        p.setIslive(false);

        p.setStatus("CREATED");

        if (req.type() == null) {
            throw new IllegalArgumentException("Property type is required");
        }
        p.setType(req.type());

        if (req.spvCompany() == null) {
            throw new IllegalArgumentException("SPV company details are required");
        }

        PropertyCreateReq.SpvCompanyReq s = req.spvCompany();

        SpvCompany spv = new SpvCompany();
        spv.setName(s.name());
        spv.setCin(s.cin());
        spv.setRegisteredAddress(s.registeredAddress());
        spv.setEmail(s.email());
        spv.setDirectorName(s.directorName());
        spv.setDirectorDin(s.directorDin());
        spv.setSigningPlace(s.signingPlace());
        spv.setBankAccountNumber(s.bankAccountNumber());
        spv.setBankIfsc(s.bankIfsc());
        spv.setBeneficiaryName(s.beneficiaryName());

        spv = spvCompanyRepo.save(spv);   // ✅ SAVED HERE

        p.setSpvCompany(spv);             // ✅ LINKED HERE
        p.setSpvId(spv.getId().toString());

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

        

        // Cloudinary upload
        List<String> uploadedUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                Map uploadResult = cloudinary.uploader().upload(image.getBytes(), ObjectUtils.emptyMap());
                uploadedUrls.add((String) uploadResult.get("url"));
            }
        }
        p.setPhotoUrls(uploadedUrls);

        List<String> uploadedVideoUrls = new ArrayList<>();
        if (videos != null && !videos.isEmpty()) {
            for (MultipartFile video : videos) {
                Map uploadResult = cloudinary.uploader().upload(
                        video.getBytes(),
                        ObjectUtils.asMap("resource_type", "video")
                );
                uploadedVideoUrls.add((String) uploadResult.get("url"));
            }
        }
        p.setVideoUrls(uploadedVideoUrls);


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
    public List<Property> getPropertiesByOwnerId(Long ownerId) {
        return repo.findByOwnerId(ownerId);
    }

    public List<Property> getOnChainProperties() {
        return repo.findByStatusIn(List.of("ON_CHAIN", "HOT_READY"));
    }

    public List<Property> getallProperties()
    {
        return repo.findByStatusIn(List.of("ON_CHAIN", "HOT_READY","CREATED"));
    }

    public List<Property> getallRentedProperty()
    {
        return repo.findByStateAndIsliveTrue(PropertyState.NORMAL);
    }
    public List<Property> getNormalPropertiesNearby(double lat, double lng) {
        return repo.findNormalPropertiesWithin6Km(lat, lng);
    }

    public List<Property> getPropByType(String type) {

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Property type is required");
        }

        PropertyType propertyType;
        try {
            propertyType = PropertyType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid property type: " + type +
                            ". Allowed values: HOUSE, APARTMENT, VILLA, OFFICE, SHOP, LAND, WAREHOUSE"
            );
        }

        return repo.findByType(propertyType);
    }

    public List<Property> getPropsByTypes(String types) {

        if (types == null || types.isBlank()) {
            throw new IllegalArgumentException("Property types are required");
        }

        List<PropertyType> typeList = Arrays.stream(types.split(","))
                .map(String::trim)
                .map(String::toUpperCase)   // 👈 converts small/mixed to CAPS
                .map(PropertyType::valueOf)
                .toList();

        return repo.findByTypeIn(typeList);
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

        if (req.monthlyRent() != null) {
            p.setMonthlyRent(req.monthlyRent());
        }
        if (req.annualRoiPercentage() != null) {
            p.setAnnualRoiPercentage(req.annualRoiPercentage());
        }
        

        return repo.save(p);
    }


    @Transactional
    public Property createNormalProperty(
            NormalPropertyCreateReq req,
            List<MultipartFile> images,
            List<MultipartFile> videos,
            Long ownerId
    ) throws Exception {

        // 🔹 Fetch Owner
        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Property p = new Property();

        // 🔹 Basic Info
        p.setName(req.name());
        p.setAbout(req.about());

        // 🔹 Coordinates
        Property.Coordinates coords = new Property.Coordinates();
        coords.setLat(req.lat());
        coords.setLng(req.lng());
        p.setCoordinates(coords);

        // 🔹 Rental Info
        p.setMonthlyRent(req.monthlyRent());
        p.setSecurity(req.security());
        p.setSize(req.size());
        p.setLocationTitle(req.locationTitle());

        // 🔹 Type & State
        p.setType(req.type());
        p.setFurnished(req.furnished());
        p.setState(PropertyState.NORMAL);   // Force NORMAL
        p.setIslive(false);                 // Admin must approve
        p.setStatus("CREATED");

        // 🔹 Link Owner (Platform User)
        p.setOwner(owner);

        // 🔹 Add Rooms (if provided)
        if (req.rooms() != null && !req.rooms().isEmpty()) {

            List<Property.Room> roomList = new ArrayList<>();

            for (NormalPropertyCreateReq.RoomReq r : req.rooms()) {
                Property.Room room = new Property.Room();
                room.setType(r.type());
                room.setValue(r.value());
                roomList.add(room);
            }

            p.setRooms(roomList);
        }

        // ---------------------------
        // 🔹 Cloudinary Image Upload
        // ---------------------------
        List<String> uploadedImageUrls = new ArrayList<>();

        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {

                Map uploadResult = cloudinary.uploader()
                        .upload(image.getBytes(), ObjectUtils.emptyMap());

                uploadedImageUrls.add((String) uploadResult.get("url"));
            }
        }

        p.setPhotoUrls(uploadedImageUrls);

        // ---------------------------
        // 🔹 Cloudinary Video Upload
        // ---------------------------
        List<String> uploadedVideoUrls = new ArrayList<>();

        if (videos != null && !videos.isEmpty()) {
            for (MultipartFile video : videos) {

                Map uploadResult = cloudinary.uploader().upload(
                        video.getBytes(),
                        ObjectUtils.asMap("resource_type", "video")
                );

                uploadedVideoUrls.add((String) uploadResult.get("url"));
            }
        }

        p.setVideoUrls(uploadedVideoUrls);

        // 🔹 Save Property
        return repo.save(p);
    }

    @Transactional
    public Property toggleLiveStatus(Long propertyId, Long ownerId) {

        Property property = repo.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new RuntimeException("Property not found or not yours"));

        property.setIslive(!property.isIslive());  // toggle

        return repo.save(property);
    }



}
