package com.product.RealestateTokenization.service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Inject the template engine

    // Use constructor injection
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // Updated method signature
    public void sendInvestmentEmail(
            String to,
            String investorName,
            String propertyName,
            String spvName,
            int shares,
            double pricePerShare,
            double totalAmount,
            java.time.LocalDate investmentDate,  // NEW
            String cinNumber                     // NEW
    ) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Investment Acknowledgement – " + propertyName + " | BITSBRICK Pvt. Ltd.");

        // Create Thymeleaf context with ALL variables
        Context context = new Context();
        context.setVariable("investorName", investorName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("spvName", spvName);
        context.setVariable("shares", shares);
        context.setVariable("pricePerShare", pricePerShare);
        context.setVariable("totalAmount", totalAmount);
        context.setVariable("investmentDate", investmentDate);     // ← Added
        context.setVariable("cinNumber", cinNumber != null ? cinNumber : "U72900DL2024PTC123456"); // fallback

        // Process template
        String html = templateEngine.process("investment-email", context);
        helper.setText(html, true);

        mailSender.send(message);
    }

    // Inside EmailService.java → add this method

    public void sendPropertyListingInterestEmail(
            String adminEmail,
            String ownerName,
            String ownerEmail,
            String ownerPhone,
            String city,
            String propertyType,
            Double approxValue,
            String message) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(adminEmail);
        helper.setSubject("New Property Listing Interest – " + ownerName + " (" + city + ")");

        Context context = new Context();
        context.setVariable("ownerName", ownerName);
        context.setVariable("ownerEmail", ownerEmail);
        context.setVariable("ownerPhone", ownerPhone);
        context.setVariable("city", city);
        context.setVariable("propertyType", propertyType != null ? propertyType : "Not specified");
        context.setVariable("approxValue", approxValue != null ? "₹" + String.format("%,.2f", approxValue) + " Lakhs" : "Not disclosed");
        context.setVariable("message", message != null ? message : "No additional message");

        String html = templateEngine.process("property-listing-interest", context);
        helper.setText(html, true);

        mailSender.send(mimeMessage);
    }
}