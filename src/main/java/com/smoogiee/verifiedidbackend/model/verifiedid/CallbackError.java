package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

@Data
public class CallbackError {
    private String code;
    private String message;
}
