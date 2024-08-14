package com.smoogiee.verifiedidbackend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

/**
 * Property bean used to retrieve Azure
 * configuration values from application-{env}.yml
 */
@Getter
public class AzureProperties {
    @Value("${entra.ad.authority}")
    private String authority;

    @Value("${entra.ad.tenant}")
    private String tenant;

    @Value("${entra.ad.managed_id}")
    private boolean managedId;

    @Value("${entra.ad.client_id}")
    private String clientId;

    @Value("${entra.ad.client_secret}")
    private String clientSecret;

    @Value("${entra.ad.client_cert_location}")
    private String clientCertLocation;

    @Value("${entra.ad.client_cert_key}")
    private String clientCertKey;

    @Value("${entra.ad.scope}")
    private String scope;
}
