package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

import java.util.List;

@Data
public class RequestedCredential {
    private String type;
    private String purpose;
    private List<String> acceptedIssuers;
    private Configuration configuration;
}
