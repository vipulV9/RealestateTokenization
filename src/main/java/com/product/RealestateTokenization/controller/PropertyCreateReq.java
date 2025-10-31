package com.product.RealestateTokenization.controller;

public record PropertyCreateReq(String name, String location, double value, int totalTokens, double tokenPrice, String spvId) {}
