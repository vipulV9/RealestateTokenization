package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.util.CryptoUtil;
import com.google.common.primitives.UnsignedInteger;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.codec.binary.XrplBinaryCodec;
import org.xrpl.xrpl4j.crypto.keys.Base58EncodedSecret;
import org.xrpl.xrpl4j.crypto.keys.KeyPair;
import org.xrpl.xrpl4j.crypto.keys.Seed;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.bc.BcSignatureService;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.SignedTransaction;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.ledger.LedgerResult;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory;
import org.xrpl.xrpl4j.model.transactions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XrplService {

    @Autowired
    private CryptoUtil cryptoUtil;


    private final XrplClient client;
    private final SignatureService signatureService = new BcSignatureService();
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    private final XrplBinaryCodec binaryCodec = new XrplBinaryCodec();

    @Value("${xrpl.hot-wallet-seed}")
    private String hotWalletSeed;

    @Value("${xrpl.issuer-address}")
    private String issuerAddress;

    public XrplService() throws Exception {
        this.client = new XrplClient(HttpUrl.get("https://s.altnet.rippletest.net:51234"));
    }

    // ---------------------------
    // WALLET CREATION & FUNDING
    // ---------------------------
    public WalletInfo createWallet() {
        Seed seed = Seed.ed25519Seed();
        KeyPair keyPair = seed.deriveKeyPair();
        String address = keyPair.publicKey().deriveAddress().value();
        //String encrypted = CryptoUtil.encrypt(keyPair.privateKey().toString());
        String encrypted = cryptoUtil.encrypt(keyPair.privateKey().toString());

        return new WalletInfo(address, encrypted);
    }

    public String fundWallet(String address) throws Exception {
        FaucetClient faucet = FaucetClient.construct(HttpUrl.get("https://faucet.altnet.rippletest.net"));
        faucet.fundAccount(FundAccountRequest.of(Address.of(address)));
        return "Wallet funded successfully via XRPL Testnet Faucet.";
    }

    // ---------------------------
    // INTERNAL HELPERS
    // ---------------------------
    private Seed getHotWalletSeed() {
        return Seed.fromBase58EncodedSecret(Base58EncodedSecret.of(hotWalletSeed));
    }

    private KeyPair getHotWalletKeyPair() {
        return getHotWalletSeed().deriveKeyPair();
    }

    private Address getHotAddress() {
        return getHotWalletKeyPair().publicKey().deriveAddress();
    }

    private XrpCurrencyAmount getFee() throws Exception {
        FeeResult feeResult = client.fee();
        return feeResult.drops().openLedgerFee();
    }

    private UnsignedInteger getSequence(Address address) throws Exception {
        AccountInfoResult info = client.accountInfo(AccountInfoRequestParams.of(address));
        return info.accountData().sequence();
    }

    private UnsignedInteger getLastLedgerSequence() throws Exception {
        LedgerResult ledger = client.ledger(LedgerRequestParams.builder()
                .ledgerSpecifier(LedgerSpecifier.VALIDATED)
                .build());
        return ledger.ledgerIndex()
                .orElseThrow(() -> new RuntimeException("No ledger index"))
                .plus(UnsignedInteger.valueOf(4))
                .unsignedIntegerValue();
    }



    // ---------------------------
    // FIXED: UNSIGNED ISSUANCE (self-send Payment) → JSON for offline signing
    // ---------------------------
    // ---------------------------
// FIXED: Generate Unsigned Transaction JSON for Offline Signing
//
    public String generateUnsignedIssuance(String currency, int amount) throws Exception {
        Address issuer = Address.of(issuerAddress);
        KeyPair keyPair = getHotWalletKeyPair();

        // fetch latest fee, sequence, and ledger info live from XRPL
        XrpCurrencyAmount fee = getFee();
        UnsignedInteger sequence = getSequence(issuer);
        UnsignedInteger lastLedgerSequence = getLastLedgerSequence();


        IssuedCurrencyAmount issued = IssuedCurrencyAmount.builder()
                .currency(currency)
                .issuer(issuer)
                .value(String.valueOf(amount))
                .build();

        Payment unsignedTx = Payment.builder()
                .account(issuer)
                .destination(issuer) // self-send issuance
                .amount(issued)
                .fee(fee)
                .sequence(sequence)
                .lastLedgerSequence(lastLedgerSequence)
                .signingPublicKey(keyPair.publicKey())
                .build();

        // build unsigned JSON for frontend
        ObjectNode txJson = mapper.createObjectNode();
        txJson.put("TransactionType", "Payment");
        txJson.put("Account", issuer.value());
        txJson.put("Destination", issuer.value());

        ObjectNode amountNode = mapper.createObjectNode();
        amountNode.put("currency", currency);
        amountNode.put("value", String.valueOf(amount));
        amountNode.put("issuer", issuer.value());
        txJson.set("Amount", amountNode);

        txJson.put("Fee", fee.value().toString());
        txJson.put("Sequence", sequence.intValue());
        txJson.put("LastLedgerSequence", lastLedgerSequence.intValue());
        txJson.put("SigningPubKey", keyPair.publicKey().base16Value());
        txJson.put("NetworkID", 21338); // testnet

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(txJson);
    }




    // ---------------------------
    // TOKEN OPERATIONS
    // ---------------------------
    public String createTrustline(String investorAddress, String currency) throws Exception {
        KeyPair issuerKeys = getHotWalletKeyPair();

        IssuedCurrencyAmount limit = IssuedCurrencyAmount.builder()
                .currency(currency)
                .issuer(Address.of(issuerAddress))
                .value("1000000")
                .build();

        TrustSet trustSet = TrustSet.builder()
                .account(Address.of(investorAddress))
                .limitAmount(limit)
                .fee(getFee())
                .sequence(getSequence(Address.of(investorAddress)))
                .signingPublicKey(issuerKeys.publicKey())
                .lastLedgerSequence(getLastLedgerSequence())
                .build();

        SingleSignedTransaction<TrustSet> signedTx = signatureService.sign(issuerKeys.privateKey(), trustSet);
        SubmitResult<TrustSet> result = client.submit(signedTx);
        return result.transactionResult().hash().value();
    }

    public String transferTokens(String to, String currency, int amount) throws Exception {
        KeyPair issuerKeys = getHotWalletKeyPair();
        Address hot = getHotAddress();

        IssuedCurrencyAmount issued = IssuedCurrencyAmount.builder()
                .currency(currency)
                .issuer(Address.of(issuerAddress))
                .value(String.valueOf(amount))
                .build();

        Payment payment = Payment.builder()
                .account(hot)
                .destination(Address.of(to))
                .amount(issued)
                .fee(getFee())
                .sequence(getSequence(hot))
                .signingPublicKey(issuerKeys.publicKey())
                .lastLedgerSequence(getLastLedgerSequence())
                .build();

        SingleSignedTransaction<Payment> signedTx = signatureService.sign(issuerKeys.privateKey(), payment);
        SubmitResult<Payment> result = client.submit(signedTx);
        return result.transactionResult().hash().value();
    }

    // ---------------------------
    // BROADCAST SIGNED BLOB (WORKING for xrpl4j 4.x)
    // Accepts a signed transaction blob hex string and submits it.
    // ---------------------------





    private final RestTemplate restTemplate = new RestTemplate();

    // Your rippled node JSON-RPC URL (e.g., testnet or mainnet)
    private final String rippledRpcUrl = "https://s.altnet.rippletest.net:51234/";

    public String broadcastSigned(String signedTxBlob) {
        try {
            // 🔹 Build request for XRPL Testnet
            Map<String, Object> request = Map.of(
                    "method", "submit",
                    "params", List.of(Map.of("tx_blob", signedTxBlob))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // 🔹 Send request to XRPL Testnet (public endpoint)
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://s.altnet.rippletest.net:51234/",
                    entity,
                    Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from XRPL node");
            }

            Map<?, ?> result = (Map<?, ?>) body.get("result");
            if (result == null) {
                throw new RuntimeException("No result field in XRPL response");
            }

            // 🔹 Handle engine results
            String engineResult = (String) result.get("engine_result");
            String engineMessage = (String) result.get("engine_result_message");

            if ("tesSUCCESS".equals(engineResult)) {
                Map<?, ?> txJson = (Map<?, ?>) result.get("tx_json");
                String txHash = txJson != null ? (String) txJson.get("hash") : "unknown";
                return "✅ Broadcast successful. Transaction hash: " + txHash;
            } else {
                return "❌ Broadcast failed — " + engineResult + ": " + engineMessage;
            }

        } catch (Exception e) {
            throw new RuntimeException("Broadcast failed: " + e.getMessage(), e);
        }
    }






    public boolean hasReserve(String address) {
        try {
            AccountInfoResult info = client.accountInfo(AccountInfoRequestParams.of(Address.of(address)));
            return new BigDecimal(String.valueOf(info.accountData().balance().value()))
                    .compareTo(new BigDecimal("10000000")) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String createInvestorTrustLine(String investorSecret, String issuerAddress, String currencyCode, String limitAmount) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> tx = new HashMap<>();
            tx.put("TransactionType", "TrustSet");
            tx.put("Account", getAddressFromSecret(investorSecret));
            tx.put("LimitAmount", Map.of(
                    "currency", currencyCode,
                    "issuer", issuerAddress,
                    "value", limitAmount
            ));
            tx.put("Fee", "12");
            tx.put("Flags", 131072); // tfSetNoRipple flag

            Map<String, Object> request = new HashMap<>();
            request.put("method", "submit");
            request.put("params", List.of(Map.of(
                    "secret", investorSecret,
                    "tx_json", tx
            )));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://s.altnet.rippletest.net:51234/",
                    entity,
                    Map.class
            );

            Map<?, ?> result = (Map<?, ?>) ((Map<?, ?>) response.getBody()).get("result");
            if (result == null) {
                return "❌ No result from XRPL while creating trustline.";
            }

            String engineResult = (String) result.get("engine_result");
            String engineMessage = (String) result.get("engine_result_message");

            if ("tesSUCCESS".equals(engineResult)) {
                return "✅ Investor trustline created successfully.";
            } else if (engineResult.startsWith("tef") || engineResult.startsWith("tec")) {
                return "⚠️ Trustline may already exist or insufficient reserve: " + engineMessage;
            } else {
                return "❌ Trustline failed: " + engineResult + " - " + engineMessage;
            }
        } catch (Exception e) {
            return "❌ Trustline creation failed: " + e.getMessage();
        }
    }

    private String getAddressFromSecret(String secret) {
        try {
            Seed seed = Seed.fromBase58EncodedSecret(Base58EncodedSecret.of(secret));
            KeyPair keyPair = seed.deriveKeyPair();
            return keyPair.publicKey().deriveAddress().value();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive address from secret: " + e.getMessage());
        }
    }

    public String transferTokensToInvestor(String investorAddress, String currencyCode, int amount) {
        try {
            KeyPair issuerKeys = getHotWalletKeyPair();
            Address issuerAddr = Address.of(issuerAddress);

            IssuedCurrencyAmount issued = IssuedCurrencyAmount.builder()
                    .currency(currencyCode)
                    .issuer(issuerAddr)
                    .value(String.valueOf(amount))
                    .build();

            Payment payment = Payment.builder()
                    .account(issuerAddr)
                    .destination(Address.of(investorAddress))
                    .amount(issued)
                    .fee(getFee())
                    .sequence(getSequence(issuerAddr))
                    .signingPublicKey(issuerKeys.publicKey())
                    .lastLedgerSequence(getLastLedgerSequence())
                    .build();

            SingleSignedTransaction<Payment> signedTx = signatureService.sign(issuerKeys.privateKey(), payment);
            SubmitResult<Payment> result = client.submit(signedTx);
            return  result.transactionResult().hash().value();
        } catch (Exception e) {
            return "❌ Token transfer failed: " + e.getMessage();
        }
    }


}

