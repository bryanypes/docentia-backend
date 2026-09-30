package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docentia.jwt")
public record JwtProperties(String secreto, long minutosVigencia) {}
