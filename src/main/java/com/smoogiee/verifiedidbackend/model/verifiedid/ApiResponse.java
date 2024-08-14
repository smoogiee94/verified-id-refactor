package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

@Data
public class ApiResponse {
    private String requestId;
    private String url;
    private int expiry;
    private String qrCode;
    private String id;
    private String pin;
}
