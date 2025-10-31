package com.product.RealestateTokenization.controller;

import com.product.RealestateTokenization.model.Property;
import com.product.RealestateTokenization.model.Transaction;
import com.product.RealestateTokenization.service.AdminActionService;
import com.product.RealestateTokenization.service.PropertyService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final PropertyService propertyService;
    private final AdminActionService adminService;

    public AdminController(PropertyService propertyService, AdminActionService adminService) {
        this.propertyService = propertyService;
        this.adminService = adminService;
    }

    @PostMapping("/properties")
    public ResponseEntity<PropertyResponse> createProperty(@RequestBody PropertyCreateReq req) throws Exception {
        Property p = propertyService.createPropertyWithUnsignedTx(req);
        return ResponseEntity.ok(new PropertyResponse(p.getId()));
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

    @PostMapping("/transactions/{id}/confirm")
    public Transaction confirm(@PathVariable Long id, @RequestHeader("adminId") Long adminId) throws Exception {
        return adminService.confirm(id, adminId);
    }

    @PostMapping("/transactions/{id}/reject")
    public Transaction reject(@PathVariable Long id) {
        return adminService.reject(id);
    }
}

record PropertyResponse(Long id) {}
record SignedTxUploadReq(String signedTxBlob) {}