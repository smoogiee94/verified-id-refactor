package com.smoogiee.verifiedidbackend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

/**
 * Property bean used to retrieve Verified ID
 * configuration values from application-{env}.yml
 */
@Getter
public class VerifiedIdProperties {
    @Value("${entra.verified_id.api_endpoint}")
    private String apiEndpoint;

    @Value("${entra.verified_id.did_authority}")
    private String didAuthority;

    @Value("${entra.verified_id.client_name}")
    private String clientName;

    // TODO: Override getter to securely randomly generate API key
    @Value("${entra.verified_id.api_key}")
    private String apiKey;

    @Value("${entra.verified_id.credential_type}")
    private String credentialType;

    @Value("${entra.verified_id.purpose}")
    private String purpose;

    @Value("${entra.verified_id.manifest_url}")
    private String manifestUrl;

    @Value("${entra.verified_id.pin_code_length}")
    private int pinCodeLength;

    @Value("${entra.verified_id.photo_claim_name}")
    private String photoClaimName;

    @Value("${entra.verified_id.use_face_check}")
    private String useFaceCheck;
}
