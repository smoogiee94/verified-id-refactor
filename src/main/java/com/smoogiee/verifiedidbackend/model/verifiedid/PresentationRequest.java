package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

import java.util.List;

@Data
public class PresentationRequest {
    private boolean includeQrCode;
    private boolean includeReceipt;
    private String authority;
    private Registration registration;
    private Callback callback;
    private List<RequestedCredential> requestedCredentials;
}
