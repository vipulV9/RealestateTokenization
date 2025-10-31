package com.product.RealestateTokenization.xrpl;

/**
 * A custom client for interacting with the XRP Ledger.
 * This is a mock implementation that doesn't actually connect to the XRPL network.
 */
public class XrplClient {
    private final String url;

    /**
     * Creates a new XrplClient with the specified URL.
     */
    public XrplClient(String url) {
        this.url = url;
    }

    private static final XrplClient testnet = new XrplClient("https://s.altnet.rippletest.net:51234");

    /**
     * Returns a client connected to the XRPL Testnet.
     */
    public static XrplClient testnet() { 
        return testnet; 
    }

    /**
     * Returns the URL this client is connected to.
     */
    public String getUrl() {
        return url;
    }
}
