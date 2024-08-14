package com.smoogiee.verifiedidbackend.model.verifiedid;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CallbackEvent {
    private String requestId;
    private String requestStatus;
    private String state;
    private String subject;
    private List<VerifiedCredentialsData> verifiedCredentialsData;
    private Receipt receipt;
    @JsonProperty("error")
    private CallbackError error;
}
