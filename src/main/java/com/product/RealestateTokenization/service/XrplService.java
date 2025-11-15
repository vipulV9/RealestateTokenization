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
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.ledger.LedgerResult;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.flags.TrustSetFlags;
import org.xrpl.xrpl4j.model.jackson.ObjectMapperFactory;
import org.xrpl.xrpl4j.model.transactions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XrplService {
    @Value("${xrpl.hot-wallet-seed}")
    private String hotWalletSeed;

    @Value("${xrpl.hot-wallet-address}")
    private String hotWalletAddress;

    @Value("${xrpl.cold-wallet-seed}")
    private String coldWalletSeed;

    @Value("${xrpl.cold-wallet-address}")
    private String coldWalletAddress;

    @Value("${xrpl.issuer-address}")
    private String issuerAddress;

    @Autowired
    private CryptoUtil cryptoUtil;

    private final XrplClient client;
    private final SignatureService signatureService = new BcSignatureService();
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    private final XrplBinaryCodec binaryCodec = new XrplBinaryCodec();


    @Value("${xrpl.rpc.url:https://s.altnet.rippletest.net:51234/}")
    private String xrplRpcUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Default safety values (can tweak)
    private static final int DEFAULT_EXTRA_LEDGERS = 120; // ~8 minutes safety window
    private static final int DEFAULT_FEE_MULTIPLIER = 3;  // fee x3 by default

    public XrplService() throws Exception {
        this.client = new XrplClient(HttpUrl.get("https://s.altnet.rippletest.net:51234"));
    }

    // ---------------------------
    // WALLET CREATION & FUNDING
    // ---------------------------
//    public WalletInfo createWallet() {
//        Seed seed = Seed.ed25519Seed();
//        KeyPair keyPair = seed.deriveKeyPair();
//        String address = keyPair.publicKey().deriveAddress().value();
//        String encrypted = cryptoUtil.encrypt(keyPair.privateKey().toString());
//        return new WalletInfo(address, encrypted);
//    }

    public WalletInfo createWallet() {
        RestTemplate rest = new RestTemplate();
        Map result = rest.getForObject("http://localhost:3001/create-wallet", Map.class);

        String address = (String) result.get("address");
        String seed = (String) result.get("seed");

        return new WalletInfo(address, seed);
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

    private UnsignedInteger getCurrentLedgerIndex() throws Exception {
        LedgerResult ledger = client.ledger(LedgerRequestParams.builder()
                .ledgerSpecifier(LedgerSpecifier.VALIDATED)
                .build());
        LedgerIndex ledgerIndex = ledger.ledgerIndex()
                .orElseThrow(() -> new RuntimeException("No ledger index present"));
        return UnsignedInteger.valueOf(ledgerIndex.unsignedIntegerValue().intValue());
    }

    /**
     * Autofill-like helper: returns a map with keys: "feeDrops" (long), "sequence" (UnsignedInteger),
     * "lastLedgerSequence" (UnsignedInteger).
     *
     * - extraLedgers: how many ledgers to add for the LastLedgerSequence window.
     * - feeMultiplier: multiply the network fee by this factor to increase priority.
     */
    private Map<String, Object> autofillForAccount(Address account, int extraLedgers, int feeMultiplier) throws Exception {
        Map<String, Object> out = new HashMap<>();

        // fetch fee
        FeeResult feeResult = client.fee();
        long baseFeeDrops = feeResult.drops().openLedgerFee().value().longValue(); // safe conversion
        long feeDrops = baseFeeDrops * Math.max(1, feeMultiplier);

        // fetch sequence
        AccountInfoResult info = client.accountInfo(AccountInfoRequestParams.of(account));
        UnsignedInteger sequence = info.accountData().sequence();

        // fetch ledger index and compute lastLedgerSequence
        LedgerResult ledger = client.ledger(LedgerRequestParams.builder()
                .ledgerSpecifier(LedgerSpecifier.VALIDATED)
                .build());
        LedgerIndex ledgerIndex = ledger.ledgerIndex()
                .orElseThrow(() -> new RuntimeException("No ledger index present"));
        UnsignedInteger currentIndex = UnsignedInteger.valueOf(ledgerIndex.unsignedIntegerValue().intValue());
        UnsignedInteger lastLedgerSequence = currentIndex.plus(UnsignedInteger.valueOf(Math.max(1, extraLedgers)));

        out.put("feeDrops", feeDrops);
        out.put("sequence", sequence);
        out.put("lastLedgerSequence", lastLedgerSequence);
        return out;
    }

    // ---------------------------
    // UNSIGNED ISSUANCE (Payment)
    // ---------------------------
    // ---------------------------
// UNSIGNED ISSUANCE (Payment)
// ---------------------------
    public String generateUnsignedIssuance(String currency, int amount) throws Exception {
        Address issuer = Address.of(issuerAddress);
        Address hotWallet = Address.of(hotWalletAddress); // ✅ Keep this for hot wallet

        // Use autofill to get fresh values (extra ledger window + fee multiplier)
        Map<String, Object> auto = autofillForAccount(issuer, DEFAULT_EXTRA_LEDGERS, DEFAULT_FEE_MULTIPLIER);
        long feeDrops = (long) auto.get("feeDrops");
        UnsignedInteger sequence = (UnsignedInteger) auto.get("sequence");
        UnsignedInteger lastLedgerSequence = (UnsignedInteger) auto.get("lastLedgerSequence");

        IssuedCurrencyAmount issued = IssuedCurrencyAmount.builder()
                .currency(currency)
                .issuer(issuer)
                .value(String.valueOf(amount))
                .build();

        // Build unsigned Payment object (do NOT put signingPublicKey here)
        Payment unsignedTx = Payment.builder()
                .account(issuer)
                .destination(hotWallet) // ✅ Send to hot wallet
                .amount(issued)
                .fee(XrpCurrencyAmount.ofDrops(feeDrops))
                .sequence(sequence)
                .lastLedgerSequence(lastLedgerSequence)
                .build();

        // build unsigned JSON for frontend / offline signing
        ObjectNode txJson = mapper.createObjectNode();
        txJson.put("TransactionType", "Payment");
        txJson.put("Account", issuer.value());
        txJson.put("Destination", hotWallet.value()); // ✅ CHANGED: Use hotWallet, not coldWallet

        ObjectNode amountNode = mapper.createObjectNode();
        amountNode.put("currency", currency);
        amountNode.put("value", String.valueOf(amount));
        amountNode.put("issuer", issuer.value());
        txJson.set("Amount", amountNode);

        txJson.put("Fee", String.valueOf(feeDrops));
        txJson.put("Sequence", sequence.intValue());
        txJson.put("LastLedgerSequence", lastLedgerSequence.intValue());

        // IMPORTANT: do NOT include SigningPubKey or NetworkID here — signer adds them
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(txJson);
    }


    // ---------------------------
    // TOKEN OPERATIONS
    // ---------------------------
    /**
     * Create unsigned trustline JSON for investor to sign (autofilled so sequence+fee fresh).
     */
    public String createTrustline(String investorAddress, String currency) throws Exception {
        Address investor = Address.of(investorAddress);
        Address issuer = Address.of(issuerAddress);

        Map<String, Object> auto = autofillForAccount(investor, DEFAULT_EXTRA_LEDGERS, DEFAULT_FEE_MULTIPLIER);
        long feeDrops = (long) auto.get("feeDrops");
        UnsignedInteger sequence = (UnsignedInteger) auto.get("sequence");
        UnsignedInteger lastLedgerSequence = (UnsignedInteger) auto.get("lastLedgerSequence");

        ObjectMapper mapper = ObjectMapperFactory.create();
        ObjectNode tx = mapper.createObjectNode();

        tx.put("TransactionType", "TrustSet");
        tx.put("Account", investor.value());
        tx.put("Fee", String.valueOf(feeDrops));
        tx.put("Sequence", sequence.intValue());
        tx.put("LastLedgerSequence", lastLedgerSequence.intValue());

        // MUST INCLUDE
        tx.put("SigningPubKey", "");        // unsigned transactions MUST have empty SigningPubKey
        tx.put("Flags", 131072);            // tfSetNoRipple OR default for TrustSet

        ObjectNode limit = mapper.createObjectNode();
        limit.put("currency", currency);
        limit.put("issuer", issuer.value());
        limit.put("value", "10000000000"); // large trust limit

        tx.set("LimitAmount", limit);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tx);
    }

    /**
     * Server-signed transfer: autofill before building and signing the Payment to avoid stale sequence/fee.
     */
    public String transferTokens(String to, String currency, int amount) throws Exception {
        KeyPair issuerKeys = getHotWalletKeyPair();
        Address hot = getHotAddress();

        Map<String, Object> auto = autofillForAccount(hot, DEFAULT_EXTRA_LEDGERS / 2, DEFAULT_FEE_MULTIPLIER); // slightly smaller window for server-signed
        long feeDrops = (long) auto.get("feeDrops");
        UnsignedInteger sequence = (UnsignedInteger) auto.get("sequence");
        UnsignedInteger lastLedgerSequence = (UnsignedInteger) auto.get("lastLedgerSequence");

        IssuedCurrencyAmount issued = IssuedCurrencyAmount.builder()
                .currency(currency)
                .issuer(Address.of(issuerAddress))
                .value(String.valueOf(amount))
                .build();

        Payment payment = Payment.builder()
                .account(hot)
                .destination(Address.of(to))
                .amount(issued)
                .fee(XrpCurrencyAmount.ofDrops(feeDrops))
                .sequence(sequence)
                .signingPublicKey(issuerKeys.publicKey())
                .lastLedgerSequence(lastLedgerSequence)
                .build();

        SingleSignedTransaction<Payment> signedTx = signatureService.sign(issuerKeys.privateKey(), payment);
        SubmitResult<Payment> result = client.submit(signedTx);
        return result.transactionResult().hash().value();
    }

    public String transferTokensToInvestor(String investorAddress, String currencyCode, int amount) {
        try {
            // HOT wallet (sender)
            KeyPair hotKeys = getHotWalletKeyPair();
            Address hot = getHotAddress();

            // COLD wallet is issuer
            Address issuer = Address.of(coldWalletAddress);
            Address investor = Address.of(investorAddress);

            // Autofill
            Map<String, Object> auto = autofillForAccount(
                    hot,
                    DEFAULT_EXTRA_LEDGERS / 2,
                    DEFAULT_FEE_MULTIPLIER
            );

            long feeDrops = (long) auto.get("feeDrops");
            UnsignedInteger sequence = (UnsignedInteger) auto.get("sequence");
            UnsignedInteger lastLedgerSequence = (UnsignedInteger) auto.get("lastLedgerSequence");

            // ✅ CORRECT issued currency
            IssuedCurrencyAmount issued = IssuedCurrencyAmount.builder()
                    .currency(currencyCode)
                    .issuer(issuer)  // COLD WALLET (token issuer)
                    .value(String.valueOf(amount))
                    .build();

            // ✅ KEY FIX: Add DeliverMin (same as Amount for direct transfer)
            IssuedCurrencyAmount deliverMin = IssuedCurrencyAmount.builder()
                    .currency(currencyCode)
                    .issuer(issuer)
                    .value(String.valueOf(amount))
                    .build();

            // Payment from HOT to investor
            Payment payment = Payment.builder()
                    .account(hot)                         // sender = hot wallet
                    .destination(investor)
                    .amount(issued)
                    .deliverMin(deliverMin)              // ✅ ADD THIS
                    .fee(XrpCurrencyAmount.ofDrops(feeDrops))
                    .sequence(sequence)
                    .lastLedgerSequence(lastLedgerSequence)
                    .signingPublicKey(hotKeys.publicKey())
                    .build();

            // Sign & submit
            SingleSignedTransaction<Payment> signedTx =
                    signatureService.sign(hotKeys.privateKey(), payment);

            SubmitResult<Payment> result = client.submit(signedTx);

            return result.transactionResult().hash().value();

        } catch (Exception e) {
            return "❌ Token transfer failed: " + e.getMessage();
        }
    }


    // ---------------------------
    // BROADCAST SIGNED BLOB
    // ---------------------------
    public String broadcastSigned(String signedTxBlob) {
        try {
            Map<String, Object> request = Map.of(
                    "method", "submit",
                    "params", List.of(Map.of("tx_blob", signedTxBlob))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(xrplRpcUrl, entity, Map.class);

            Map<?, ?> body = response.getBody();
            if (body == null || !body.containsKey("result")) {
                throw new RuntimeException("Empty or invalid response from XRPL node");
            }

            Map<?, ?> result = (Map<?, ?>) body.get("result");
            String engineResult = (String) result.get("engine_result");
            Map<?, ?> txJson = (Map<?, ?>) result.get("tx_json");
            String txHash = txJson != null ? (String) txJson.get("hash") : "unknown";

            if (!"tesSUCCESS".equals(engineResult)) {
                return "❌ Broadcast failed — " + engineResult + ": " + result.get("engine_result_message");
            }

            System.out.println("✅ Submitted TX Hash: " + txHash);
            int retries = 0;
            boolean validated = false;

            while (retries < 15) { // ~15 checks (~60 seconds)
                Thread.sleep(3000);
                String status = checkTransactionStatus(txHash);
                if (status.contains("validated=true")) {
                    validated = true;
                    break;
                }
                retries++;
            }

            if (validated) {
                return "✅ Transaction validated successfully! Hash: " + txHash;
            } else {
                return "⚠️ Transaction not validated within time. Possibly expired.\n" +
                        "Regenerate new unsigned issuance from backend and sign again.";
            }

        } catch (Exception e) {
            throw new RuntimeException("Broadcast failed: " + e.getMessage(), e);
        }
    }

    // ---------------------------
    // CHECK TRANSACTION STATUS
    // ---------------------------
    public String checkTransactionStatus(String txHash) {
        try {
            Map<String, Object> params = Map.of(
                    "method", "tx",
                    "params", List.of(Map.of("transaction", txHash, "binary", false))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(xrplRpcUrl, entity, Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) return "No response from XRPL node";

            Map<?, ?> result = (Map<?, ?>) body.get("result");
            if (result == null) {
                return "No result for tx: " + txHash;
            }

            Boolean validated = (Boolean) result.get("validated");
            String engineResult = result.get("engine_result") != null ? result.get("engine_result").toString() : "unknown";
            String engineMessage = result.get("engine_result_message") != null ? result.get("engine_result_message").toString() : "";

            StringBuilder sb = new StringBuilder();
            sb.append("txHash=").append(txHash)
                    .append(", validated=").append(validated)
                    .append(", engine_result=").append(engineResult);

            if (!engineMessage.isEmpty()) {
                sb.append(", message=").append(engineMessage);
            }

            return sb.toString();
        } catch (Exception e) {
            return "checkTransactionStatus failed: " + e.getMessage();
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
            // 1. Decode secret -> keypair
            Seed seed = Seed.fromBase58EncodedSecret(Base58EncodedSecret.of(investorSecret));
            KeyPair keyPair = seed.deriveKeyPair();
            Address investor = keyPair.publicKey().deriveAddress();

            // 2. Autofill values
            Map<String, Object> auto = autofillForAccount(investor, 20, 2);
            long feeDrops = (long) auto.get("feeDrops");
            UnsignedInteger sequence = (UnsignedInteger) auto.get("sequence");
            UnsignedInteger lastLedgerSequence = (UnsignedInteger) auto.get("lastLedgerSequence");

            // 3. Build TrustSet transaction
            TrustSet trust = TrustSet.builder()
                    .account(investor)
                    .fee(XrpCurrencyAmount.ofDrops(feeDrops))
                    .sequence(sequence)
                    .lastLedgerSequence(lastLedgerSequence)
                    .limitAmount(
                            IssuedCurrencyAmount.builder()
                                    .currency(currencyCode)
                                    .issuer(Address.of(issuerAddress))
                                    .value(limitAmount)
                                    .build()
                    )
                    .flags(TrustSetFlags.of(0x00020000L))   // ⭐ ENABLE RIPPLING (CLEAR NoRipple)
                    .signingPublicKey(keyPair.publicKey())
                    .build();


            // 4. SIGN OFFLINE using xrpl4j
            SignatureService service = new BcSignatureService();
            SingleSignedTransaction<TrustSet> signed = service.sign(keyPair.privateKey(), trust);

            // 5. Submit signed blob
            SubmitResult<TrustSet> result = client.submit(signed);

            if (!"tesSUCCESS".equals(result.engineResult())) {
                return "❌ Trustline failed: " + result.engineResult()
                        + " - " + result.engineResultMessage();
            }

            return "Trustline OK. Hash: " + result.transactionResult().hash().value();

        } catch (Exception e) {
            return "Trustline creation failed: " + e.getMessage();
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
    public boolean checkTrustlineExists(String accountAddress, String issuerAddress, String currency) {
        try {
            Map<String, Object> params = Map.of(
                    "method", "account_lines",
                    "params", List.of(Map.of(
                            "account", accountAddress,
                            "ledger_index", "validated",
                            "limit", 100
                    ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(xrplRpcUrl, entity, Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) return false;

            Map<?, ?> result = (Map<?, ?>) body.get("result");
            if (result == null) return false;

            List<?> lines = (List<?>) result.get("lines");
            if (lines == null) return false;

            for (Object o : lines) {
                Map<?, ?> line = (Map<?, ?>) o;
                String cur = (String) line.get("currency");
                String issuer = (String) line.get("account"); // note: account field in account_lines is issuer
                if (currency.equals(cur) && issuerAddress.equals(issuer)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            // log e
            return false;
        }
    }
    public String getHotWalletSecret() {
        return hotWalletSeed;
    }

    public String getHotWalletAddress() {
        return hotWalletAddress;
    }

    public String getColdWalletSecret() {
        return coldWalletSeed;
    }

    public String getColdWalletAddress() {
        return coldWalletAddress;
    }

    public String getIssuerAddressString() {
        return issuerAddress;
    }

    public String createHotWalletTrustLine(String currencyCode) {
        return createInvestorTrustLine(
                hotWalletSeed,              // Hot wallet secret
                coldWalletAddress,          // ✅ Trust the COLD wallet (issuer)
                currencyCode,
                "10000000000"
        );
    }



}
