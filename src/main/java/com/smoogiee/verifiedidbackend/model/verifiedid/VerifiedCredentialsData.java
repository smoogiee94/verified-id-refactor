package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

import java.util.List;

@Data
public class VerifiedCredentialsData {
    private String issuer;
    private List<String> type;
    private Claims claims;
    private CredentialState credentialState;
    private DomainValidation domainValidation;
    private String issuanceDate;
    private String expirationDate;
}
