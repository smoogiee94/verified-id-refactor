package com.smoogiee.verifiedidbackend.model.verifiedid;

import lombok.Data;

@Data
public class Callback {
    private String url;
    private String state;
    private Headers headers;
}
