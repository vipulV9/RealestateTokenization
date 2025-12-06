package com.product.RealestateTokenization.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    // EmailService.java

    public void sendInterestEmail(
            String to,
            String name,
            String propertyName,
            String tokenSymbol,
            int tokens,
            double tokenPrice,
            double totalAmount,
            String paymentLinkUrl,
            boolean feeIncluded) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Your Investment Interest – " + propertyName);

        String feeNote = feeIncluded
                ? "<p style='color:#e74c3c; font-weight:bold;'>Note: ₹800 one-time wallet creation fee included</p>"
                : "";

        String html = """
        <div style="font-family:Arial,sans-serif; max-width:600px; margin:auto; padding:20px; border:1px solid #ddd; border-radius:10px; background:#f9f9f9;">
          <h2 style="color:#2c3e50;">Hi %s,</h2>
          <p>Thank you for showing interest in <strong>%s (%s)</strong>!</p>
          <p>You requested: <strong>%d tokens</strong> at <strong>₹%.2f each</strong></p>
          <h3 style="color:#27ae60;">Total Amount: ₹%.2f</h3>
          %s
          <p>Please complete your payment using the secure link below:</p>
          <div style="text-align:center; margin:40px 0;">
            <a href="%s" style="background:#e67e22; color:white; padding:16px 36px; text-decoration:none; border-radius:8px; font-size:18px; font-weight:bold;">
              Pay ₹%.2f Now
            </a>
          </div>
          <p style="color:#7f8c8d;"><small>This payment link expires in 7 days.</small></p>
          <hr>
          <small style="color:#95a5a6;">Ref: Investment in %s • %d tokens • %s</small>
        </div>
        """.formatted(
                name,                    // 1
                propertyName,            // 2
                tokenSymbol,             // 3
                tokens,                  // 4
                tokenPrice,              // 5
                totalAmount,             // 6
                feeNote,                 // 7
                paymentLinkUrl,          // 8
                totalAmount,             // 9
                propertyName,            // 10
                tokens,                  // 11
                tokenSymbol              // 12
        );

        helper.setText(html, true);
        mailSender.send(message);
    }
}
