package com.smoogiee.verifiedidbackend.model.verifiedid;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Headers {
    @JsonProperty("api-key")
    private String apiKey;
}
