package com.product.RealestateTokenization.service;

import com.product.RealestateTokenization.util.CryptoUtil;
import com.google.common.primitives.UnsignedInteger;
import okhttp3.HttpUrl;
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
import java.util.List;
import java.util.Map;

@Service
public class XrplService {

    private final XrplClient client;
    private final SignatureService signatureService = new BcSignatureService();
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    private final XrplBinaryCodec binaryCodec = new XrplBinaryCodec();

    @Value("${xrpl.hot-wallet-seed}")
    private String hotWalletSeed;

    @Value("${xrpl.issuer-address}")
    private String issuerAddress;

    public XrplService(XrplClient client) {
        this.client = client;
    }

    // ---------------------------
    // WALLET CREATION & FUNDING
    // ---------------------------
    public WalletInfo createWallet() {
        Seed seed = Seed.ed25519Seed();
        KeyPair keyPair = seed.deriveKeyPair();
        String address = keyPair.publicKey().deriveAddress().value();
        String encrypted = CryptoUtil.encrypt(keyPair.privateKey().toString());
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

        // ✅ Always fetch fresh sequence before building
        UnsignedInteger nextSeq = getSequence(issuer);
        UnsignedInteger nextLedger = getLastLedgerSequence();

        IssuedCurrencyAmount issued = IssuedCurrencyAmount.builder()
                .currency(currency)
                .issuer(issuer)
                .value(String.valueOf(amount))
                .build();

        Payment unsigned = Payment.builder()
                .account(issuer)
                .destination(issuer)
                .amount(issued)
                .fee(getFee())
                .sequence(nextSeq)
                .lastLedgerSequence(nextLedger)
                .signingPublicKey(keyPair.publicKey())
                .build();

        ObjectNode txJson = mapper.createObjectNode();
        txJson.put("TransactionType", "Payment");
        txJson.put("Account", issuer.value());
        txJson.put("Destination", issuer.value());

        ObjectNode amountNode = mapper.createObjectNode();
        amountNode.put("currency", currency);
        amountNode.put("value", String.valueOf(amount));
        amountNode.put("issuer", issuer.value());
        txJson.set("Amount", amountNode);

        txJson.put("Fee", getFee().value().toString());
        txJson.put("Sequence", nextSeq.intValue());
        txJson.put("LastLedgerSequence", nextLedger.intValue());
        txJson.put("SigningPubKey", keyPair.publicKey().base16Value());

        return mapper.writeValueAsString(txJson);
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
            Map<String, Object> request = Map.of(
                    "method", "submit",
                    "params", List.of(Map.of("tx_blob", signedTxBlob))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(rippledRpcUrl, entity, Map.class);

            Map<?, ?> body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from rippled node");
            }

            Map<?, ?> result = (Map<?, ?>) body.get("result");
            if ("tesSUCCESS".equals(result.get("engine_result"))) {
                Map<?, ?> txJson = (Map<?, ?>) result.get("tx_json");
                return "Broadcast successful. Transaction hash: " + txJson.get("hash");
            } else {
                throw new RuntimeException("Broadcast failed: " + result.get("engine_result_message"));
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
}