package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

@Data
public class FaceCheck {
    private String sourcePhotoClaimName;
    private int matchConfidenceThreshold;
}
