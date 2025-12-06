package com.product.RealestateTokenization.service;


import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.verify-service-sid}")
    private String verifyServiceSid;

    public void init() {
        Twilio.init(accountSid, authToken);
    }

    // Send OTP via Twilio Verify (recommended)
    public boolean sendOtp(String phoneNumber) {
        try {
            init();
            Verification verification = Verification.creator(
                            verifyServiceSid,
                            phoneNumber.startsWith("+") ? phoneNumber : "+91" + phoneNumber,
                            "sms")
                    .create();

            return "approved".equals(verification.getStatus()) || "pending".equals(verification.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verify OTP
//    public boolean verifyOtp(String phoneNumber, String code) {
//        try {
//            init();
//            VerificationCheck verificationCheck = VerificationCheck.creator(
//                            verifyServiceSid
//                    )
//                    .setTo(phoneNumber.startsWith("+") ? phoneNumber : "+91" + phoneNumber)
//                    .create();
//
//            return "approved".equals(verificationCheck.getStatus());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
    public boolean verifyOtp(String phoneNumber, String code) {
        try {
            init();  // or remove if you have @PostConstruct

            String to = phoneNumber.startsWith("+") ? phoneNumber : "+91" + phoneNumber;

            VerificationCheck verificationCheck = VerificationCheck.creator(verifyServiceSid)
                    .setTo(to)
                    .setCode(code)                 // ← THIS LINE WAS MISSING!!!
                    .create();

            String status = verificationCheck.getStatus();
            System.out.println("Twilio Verify status: " + status);  // ← add this for debugging

            return "approved".equals(status);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}