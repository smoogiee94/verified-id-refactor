package com.smoogiee.verifiedidbackend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smoogiee.verifiedidbackend.model.verifiedid.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class MockVerifiedIdController {
    private final ObjectMapper objectMapper;

    @Autowired
    public MockVerifiedIdController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping(
            path = "/verifiableCredentials/createIssuanceRequest",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<String> issueRequest(HttpServletRequest request, @RequestHeader HttpHeaders headers) throws JsonProcessingException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(objectMapper.writeValueAsString(new ApiResponse()));
    }
}
