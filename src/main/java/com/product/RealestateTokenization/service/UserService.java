package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.model.*;
import com.product.RealestateTokenization.repository.PropertyRepo;
import com.product.RealestateTokenization.repository.TransactionRepo;
import com.product.RealestateTokenization.repository.UserRepo;
import com.product.RealestateTokenization.xrpl.RazorpayPayoutService;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService; // <-- Inject JwtService

    @Autowired
    RazorpayPayoutService razorpayPayoutService;

    public UserService(UserRepo userRepo, PasswordEncoder encoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    // Existing: Email/Password Register
    public User register(String name, String email, String phone, String password) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(encoder.encode(password));
        user.setKycStatus("PENDING");
        user.setCustodyMode("NON_CUSTODIAL");
        user.setRole("INVESTOR");
        return userRepo.save(user);
    }

    // Existing: Login
    public String login(String email, String password) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (encoder.matches(password, user.getPasswordHash())) {
            return jwtService.generateToken(user); // <-- Return real JWT token
        }
        throw new RuntimeException("Invalid login");
    }

    // NEW: Find by email
    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    // NEW: Register via Google
    public User registerViaGoogle(String email, String name, String googleId) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(encoder.encode(googleId)); // dummy hash
        user.setRole("INVESTOR");
        user.setKycStatus("PENDING");
        user.setCustodyMode("NON_CUSTODIAL");
        return userRepo.save(user);
    }

    public User getUser(Long id) {
        return userRepo.findById(id).orElseThrow();
    }


    @Autowired
    private TransactionRepo txRepo;

    @Autowired
    private PropertyRepo propertyRepo;


    public List<UserInvestmentDTO> getAllUsersInvestmentDetails() {

        List<User> users = userRepo.findAll();

        return users.stream().map(user -> {

            // Fetch successful transactions of this user
            List<Transaction> txns = txRepo.findByUserIdAndStatus(
                    user.getId(), TransactionStatus.COMPLETED
            );

            double totalInvested = txns.stream()
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            // Group by property
            Map<Long, List<Transaction>> byProperty =
                    txns.stream().collect(Collectors.groupingBy(Transaction::getPropertyId));

            // Build holdings list
            List<UserInvestmentDTO.PropertyHolding> holdings =
                    byProperty.entrySet().stream().map(entry -> {

                                Long propertyId = entry.getKey();
                                List<Transaction> propTxns = entry.getValue();

                                Property p = propertyRepo.findById(propertyId).orElse(null);
                                if (p == null) return null;

                                int tokensOwned = propTxns.stream()
                                        .mapToInt(Transaction::getTokens)
                                        .sum();

                                double investedAmount = propTxns.stream()
                                        .mapToDouble(Transaction::getAmount)
                                        .sum();

                                double currentValue = tokensOwned * p.getTokenPrice();

                                return new UserInvestmentDTO.PropertyHolding(
                                        p.getId(),
                                        p.getName(),
                                        p.getTokenSymbol(),
                                        tokensOwned,
                                        p.getTokenPrice(),
                                        investedAmount,
                                        currentValue
                                );
                            })
                            .filter(Objects::nonNull)
                            .toList();

            // Calculate total portfolio value (sum of currentValue of each holding)
            double portfolioValue = holdings.stream()
                    .mapToDouble(UserInvestmentDTO.PropertyHolding::currentValue)
                    .sum();

            return new UserInvestmentDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getKycStatus(),
                    user.getCustodyMode(),
                    totalInvested,
                    portfolioValue,
                    holdings
            );

        }).toList();
    }

    // Add this method to UserService.java (reuse the same logic as admin)

    public UserInvestmentDTO getUserInvestmentDetails(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Transaction> txns = txRepo.findByUserIdAndStatus(userId, TransactionStatus.COMPLETED);

        double totalInvested = txns.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        Map<Long, List<Transaction>> byProperty = txns.stream()
                .collect(Collectors.groupingBy(Transaction::getPropertyId));

        List<UserInvestmentDTO.PropertyHolding> holdings = byProperty.entrySet().stream()
                .map(entry -> {
                    Long propertyId = entry.getKey();
                    List<Transaction> propTxns = entry.getValue();

                    Property p = propertyRepo.findById(propertyId).orElse(null);
                    if (p == null) return null;

                    int tokensOwned = propTxns.stream().mapToInt(Transaction::getTokens).sum();
                    double investedAmount = propTxns.stream().mapToDouble(Transaction::getAmount).sum();
                    double currentValue = tokensOwned * p.getTokenPrice();

                    return new UserInvestmentDTO.PropertyHolding(
                            p.getId(),
                            p.getName(),
                            p.getTokenSymbol(),
                            tokensOwned,
                            p.getTokenPrice(),
                            investedAmount,
                            currentValue
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        double portfolioValue = holdings.stream()
                .mapToDouble(UserInvestmentDTO.PropertyHolding::currentValue)
                .sum();

        return new UserInvestmentDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getKycStatus(),
                user.getCustodyMode(),
                totalInvested,
                portfolioValue,
                holdings
        );
    }
    @Transactional
    public void updateBankDetails(Long userId, String accountNumber, String ifsc, String beneficiaryName) throws Exception {

        User user = getUser(userId);
        user.setBankAccountNumber(accountNumber);
        user.setBankIfsc(ifsc);
        user.setBeneficiaryName(beneficiaryName);

        // -------------------
        // CREATE CONTACT FIRST
        // -------------------
        JSONObject contact = new JSONObject();
        contact.put("name", user.getName());
        contact.put("email", user.getEmail());
        contact.put("contact", user.getPhone());
        contact.put("type", "customer");
        contact.put("reference_id", "user_" + userId);

        JSONObject contactResponse = razorpayPayoutService.createContact(contact);
        String contactId = contactResponse.getString("id");

        // -------------------------
        // CREATE FUND ACCOUNT REST
        // -------------------------
        JSONObject bankAccount = new JSONObject();
        bankAccount.put("account_number", accountNumber);
        bankAccount.put("ifsc", ifsc);
        bankAccount.put("beneficiary_name", beneficiaryName);

        JSONObject fundRequest = new JSONObject();
        fundRequest.put("contact_id", contactId);
        fundRequest.put("account_type", "bank_account");
        fundRequest.put("bank_account", bankAccount);

        JSONObject fundResponse = razorpayPayoutService.createFundAccount(fundRequest);
        String fundAccountId = fundResponse.getString("id");

        // save Razorpay fund account ID
        user.setRazorpayFundAccountId(fundAccountId);
        userRepo.save(user);
    }
    @Transactional
    public void markPhoneAsVerified(String fullPhoneNumber) {
        // Normalize: remove +, spaces
        String cleanPhone = fullPhoneNumber.replaceAll("[^0-9]", "");

        userRepo.findByPhoneEndingWith(cleanPhone.substring(cleanPhone.length() - 10))
                .ifPresent(user -> {
                    user.setPhoneVerified(true);
                    userRepo.save(user);
                });
    }

}
