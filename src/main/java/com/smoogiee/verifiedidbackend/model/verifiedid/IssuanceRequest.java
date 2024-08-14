package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

import java.util.Date;

@Data
public class IssuanceRequest {
    private boolean includeQRCode;
    private boolean includeReceipt;
    private Callback callback;
    private String authority;
    private Registration registration;
    private String type;
    private String manifest;
    private Pin pin;
    private Claims claims;
    private Date expirationDate;
}
