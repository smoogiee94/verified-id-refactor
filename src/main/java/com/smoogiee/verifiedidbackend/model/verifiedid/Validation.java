package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

@Data
public class Validation {
    private boolean allowRevoked;
    private boolean validateLinkedDomain;
    private FaceCheck faceCheck;
}
