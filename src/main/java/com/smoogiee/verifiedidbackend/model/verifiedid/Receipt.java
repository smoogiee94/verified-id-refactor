package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

@Data
public class Receipt {
    private String id_token;
    private String vp_token;
    private String state;
}
