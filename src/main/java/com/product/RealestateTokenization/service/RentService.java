package com.product.RealestateTokenization.service;


import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.*;
import com.product.RealestateTokenization.xrpl.RazorpayPayoutService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RentService {

    private final PropertyRepo propertyRepo;
    private final TokenOwnershipRepo ownershipRepo;
    private final UserRepo userRepo;
    private final TransactionRepo transactionRepo;
    private final RentDistributionRepo rentDistributionRepo;

    @Value("${razorpay.payout-account-number}")  // Your RazorpayX account number for payouts
    private String payoutAccountNumber;

    @Autowired
    RazorpayPayoutService razorpayPayoutService;

    public RentService(PropertyRepo propertyRepo, TokenOwnershipRepo ownershipRepo, UserRepo userRepo,
                       TransactionRepo transactionRepo, RentDistributionRepo rentDistributionRepo) {
        this.propertyRepo = propertyRepo;
        this.ownershipRepo = ownershipRepo;
        this.userRepo = userRepo;
        this.transactionRepo = transactionRepo;
        this.rentDistributionRepo = rentDistributionRepo;
    }

    @Transactional
    public RentDistribution distributeRent(Long propertyId) throws Exception {

        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (property.getMonthlyRent() <= 0) {
            throw new IllegalStateException("No rent set for this property");
        }

        // Snapshot ownership
        List<TokenOwnership> ownerships = ownershipRepo.findByPropertyId(propertyId);
        if (ownerships.isEmpty()) {
            throw new IllegalStateException("No investors found for this property");
        }

        int totalTokens = property.getTotalTokens();
        double totalRent = property.getMonthlyRent();

        // Create distribution record
        RentDistribution distribution = new RentDistribution();
        distribution.setPropertyId(propertyId);
        distribution.setDistributionDate(LocalDate.now());
        distribution.setTotalRentDistributed(totalRent);
        distribution.setStatus("PENDING");
        rentDistributionRepo.save(distribution);

        List<Transaction> payouts = new ArrayList<>();

        for (TokenOwnership ownership : ownerships) {

            User user = userRepo.findById(ownership.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getRazorpayFundAccountId() == null) {
                continue; // skip if no bank details
            }

            double share = (ownership.getTokens() / (double) totalTokens) * totalRent;
            if (share < 100) {  // minimum ₹100 to payout
                continue;
            }

            int amountPaise = (int) (share * 100);

            try {
                // Razorpay Payout request
                JSONObject payoutRequest = new JSONObject();
                payoutRequest.put("account_number", payoutAccountNumber);
                payoutRequest.put("fund_account_id", user.getRazorpayFundAccountId());
                payoutRequest.put("amount", amountPaise);
                payoutRequest.put("currency", "INR");

                // TEST mode → IMPS FAILS → use NEFT
                payoutRequest.put("mode", "NEFT");

                payoutRequest.put("purpose", "rent_distribution");
                payoutRequest.put("queue_if_low_balance", true);
                payoutRequest.put("reference_id", "rent_" + propertyId + "_" + user.getId());
                payoutRequest.put("narration", "Rent for " + property.getName());

                // CALL REST API
                JSONObject payout = razorpayPayoutService.createRazorpayPayout(payoutRequest);
                String payoutId = payout.getString("id");

                // Save transaction record
                Transaction tx = new Transaction();
                tx.setUserId(user.getId());
                tx.setPropertyId(propertyId);
                tx.setAmount(share);
                tx.setStatus(TransactionStatus.RENT_DISTRIBUTION);
                tx.setRazorpayPaymentId(payoutId);
                tx.setRentDistribution(distribution);
                transactionRepo.save(tx);

                payouts.add(tx);

            } catch (Exception e) {
                // mark as partial failure but continue for next user
                distribution.setStatus("PARTIAL_FAILED");
                e.printStackTrace();
            }
        }

        // final status
        distribution.setPayouts(payouts);
        distribution.setStatus(
                payouts.size() == ownerships.size() ? "COMPLETED" : "PARTIAL_FAILED"
        );

        rentDistributionRepo.save(distribution);

        return distribution;
    }

}
