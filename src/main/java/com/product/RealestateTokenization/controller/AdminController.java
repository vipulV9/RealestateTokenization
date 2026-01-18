package com.product.RealestateTokenization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    XrplService xrplService;

    @Autowired
    UserService userService;

    @Autowired
    WalletService walletService;



    @Autowired
    private final PropertyService propertyService;
    private final AdminActionService adminService;
    private  RentService rentService;

    public AdminController(PropertyService propertyService, AdminActionService adminService) {
        this.propertyService = propertyService;
        this.adminService = adminService;
    }

//    @PostMapping("/properties")
//    public ResponseEntity<PropertyResponse> createProperty(@RequestBody PropertyCreateReq req) throws Exception {
//        Property p = propertyService.createPropertyWithUnsignedTx(req);
//        return ResponseEntity.ok(new PropertyResponse(p.getId()));
//    }
@GetMapping("/properties")
public List<Property> listProperties() {
    return propertyService.getallProperties();
}

@PostMapping(
        value = "/properties",
        consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }
)
public ResponseEntity<PropertyResponse> createProperty(
        @RequestPart("property") String propertyJson,
        @RequestPart(value = "images", required = false) List<MultipartFile> images
) throws Exception {

    ObjectMapper mapper = new ObjectMapper();
    PropertyCreateReq req = mapper.readValue(propertyJson, PropertyCreateReq.class);

    Property property = propertyService.createPropertyWithUnsignedTx(req, images);
    return ResponseEntity.ok(new PropertyResponse(property.getId()));
}

@PatchMapping("/properties/{id}")
    public ResponseEntity<Property> updateProperty(
            @PathVariable Long id,
            @RequestBody PropertyUpdateReq req
    ) {
        return ResponseEntity.ok(propertyService.updateProperty(id, req));
    }






    @GetMapping("/properties/{id}/download-unsigned")
    public ResponseEntity<ByteArrayResource> downloadUnsigned(@PathVariable Long id) {
        Property p = propertyService.getProperty(id);
        if (p.getUnsignedTxBlob() == null) {
            throw new IllegalStateException("No unsigned transaction available");
        }

        byte[] jsonBytes = p.getUnsignedTxBlob().getBytes();
        ByteArrayResource resource = new ByteArrayResource(jsonBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=unsigned_issuance_" + id + ".json")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(jsonBytes.length)
                .body(resource);
    }

//    @PostMapping("/properties/{id}/upload-signed")
//    public ResponseEntity<Property> uploadSigned(@PathVariable Long id, @RequestBody SignedTxUploadReq req) throws Exception {
//        Property p = propertyService.broadcastSignedIssuance(id, req.signedTxBlob());
//        return ResponseEntity.ok(p);
//    }

    @PostMapping("/properties/{id}/upload-signed")
    public ResponseEntity<Property> uploadSigned(@PathVariable Long id, @RequestBody SignedTxUploadReq req) throws Exception {
        Property p = propertyService.broadcastSignedIssuance(id, req.signedTxBlob());
        return ResponseEntity.ok(p);
    }

    public static record SignedTxUploadReq(String signedTxBlob) {}


    @GetMapping("/pending-payments")
    public List<Transaction> pendingPayments() {
        return adminService.getPendingPayments();
    }

    @GetMapping("/allTxn")
    public List<Transaction> getalltransaction()
    {
        return adminService.getAllTransaction();
    }



    @PostMapping("/transactions/{id}/confirm")
    public Transaction confirm(@PathVariable Long id, @RequestHeader("adminId") Long adminId) throws Exception {
        return adminService.confirm(id, adminId);
    }



    @PostMapping("/transactions/{id}/reject")
    public Transaction reject(@PathVariable Long id) {
        return adminService.reject(id);
    }
    @GetMapping("/issuer/set-default-ripple/download-unsigned")
    public ResponseEntity<ByteArrayResource> downloadUnsignedDefaultRipple() throws Exception {
        String unsignedJson = xrplService.generateUnsignedDefaultRipple();
        byte[] jsonBytes = unsignedJson.getBytes();
        ByteArrayResource resource = new ByteArrayResource(jsonBytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=unsigned_default_ripple.json")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(jsonBytes.length)
                .body(resource);
    }


    @PostMapping("/issuer/set-default-ripple/upload-signed")
    public ResponseEntity<String> uploadSignedDefaultRipple(@RequestBody SignedTxUploadReq req) throws Exception {
        String result = xrplService.broadcastSigned(req.signedTxBlob());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/full-details")
    public ResponseEntity<List<UserInvestmentDTO>> getAllUsersInvestments() {
        return ResponseEntity.ok(userService.getAllUsersInvestmentDetails());
    }

    @GetMapping("/wallet")
    public Wallet getWallet(@RequestHeader("userId") Long userId) {
        return walletService.getWallet(userId);
    }

    @GetMapping("/allWallet")
    public List<Wallet> getallwallet()
    {
        return  walletService.getAllWallet();
    }




    // Add endpoint
    @PostMapping("/properties/{id}/distribute-rent")
    public ResponseEntity<RentDistribution> distributePropertyRent(@PathVariable Long id) throws Exception {
        RentDistribution distribution = rentService.distributeRent(id);
        return ResponseEntity.ok(distribution);
    }

}

record PropertyResponse(Long id) {}
record SignedTxUploadReq(String signedTxBlob) {}
record ChatSession(String sessionId, String investorName, List<ChatMessage> messages) {}
