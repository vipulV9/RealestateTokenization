package com.product.RealestateTokenization.controller;

import com.product.RealestateTokenization.service.XrplService;
import com.product.RealestateTokenization.service.WalletInfo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/xrpl")
public class XrplController {

    private final XrplService xrplService;

    public XrplController(XrplService xrplService) {
        this.xrplService = xrplService;
    }
//
//    @PostMapping("/create-wallet")
//    public WalletInfo createWallet() {
//        return xrplService.createWallet();
//    }

    @PostMapping("/broadcast-signed")
    public String broadcast(@RequestBody SignedTx req) throws Exception {
        return xrplService.broadcastSigned(req.signedTx());
    }
}

// ✅ Keep only this record here
record SignedTx(String signedTx) {}
