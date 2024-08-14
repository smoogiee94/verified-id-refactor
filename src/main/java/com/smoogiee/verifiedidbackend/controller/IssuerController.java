package com.smoogiee.verifiedidbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smoogiee.verifiedidbackend.exception.AuthenticationException;
import com.smoogiee.verifiedidbackend.model.verifiedid.ApiResponse;
import com.smoogiee.verifiedidbackend.model.verifiedid.Claims;
import com.smoogiee.verifiedidbackend.model.verifiedid.IssuanceRequest;
import com.smoogiee.verifiedidbackend.service.CacheService;
import com.smoogiee.verifiedidbackend.service.VerifiedIdService;
import com.smoogiee.verifiedidbackend.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller class used for issuing Verified IDs
 */
@Slf4j
@RestController
@EnableCaching
public class IssuerController {
    private final ObjectMapper objectMapper;
    private final CacheService<String, String> cacheService;
    private final VerifiedIdService verifiedIdService;

    /**
     * Constructor
     *
     * @param objectMapper A fasterXML Jackson ObjectMapper used for marshaling JSON objects
     * @param cacheService A cache service with strings as key:value pairs
     * @param verifiedIdService A VerifiedIdService bean used to make calls into Microsoft Verified ID
     */
    @Autowired
    public IssuerController(ObjectMapper objectMapper,
                            CacheService<String, String> cacheService,
                            VerifiedIdService verifiedIdService) {
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.verifiedIdService = verifiedIdService;
    }

    /**
     * Issue request endpoint. This endpoint is called whenever a new Verified ID issuance is requested.
     *
     * @param request The issuance request from the UI
     * @param headers The request headers
     * @return A ResponseEntity object representing the Verified ID API response
     */
    @PostMapping(
            path = "/api/issuer/request",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<String> issueRequest(HttpServletRequest request, @RequestHeader HttpHeaders headers) {
        // Log the request
        LogUtils.logHttpRequest(request);

        // Prepare response body
        String responseBody;

        try {
            // Retrieve request body and
            // marshal into claims object
            Claims claims = objectMapper.readValue(request.getReader(), Claims.class);

            // Create Issuance Request
            IssuanceRequest issuanceRequest = verifiedIdService.createIssuanceRequest(request, claims);
            String correlationId = issuanceRequest.getCallback().getState();
            String payload = objectMapper
                    .writer()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(issuanceRequest);

            // Print debug payload
            log.debug(payload);

            // Cache request correlation id and associated status
            // This is required for when Microsoft issues the issuance callback
            ObjectNode data = objectMapper.createObjectNode();
            data.put("status", "request_created");
            data.put("message", "Waiting for QR code to be scanned");
            String cachedData = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data);
            cacheService.put(correlationId, cachedData);

            // Call Verified ID API
            responseBody = verifiedIdService.initiateIssuanceRequest(issuanceRequest);

            // Marshal Verified ID API response
            ApiResponse issuanceResponse = objectMapper.readValue(responseBody, ApiResponse.class);
            issuanceResponse.setId(correlationId);
            if (issuanceRequest.getPin() != null) {
                issuanceResponse.setPin(issuanceRequest.getPin().getValue());
            }
            responseBody = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(issuanceResponse);

            // Print debug Verified ID API response
            log.debug(responseBody);

            // Print debug cached data
            log.debug(cachedData);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Request may be malformed.");
        } catch (AuthenticationException ex) {
            log.error(ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal authentication failed.");
        }

        // Set up and return response
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(responseBody);
    }

    // TODO: Implement getManifest method whenever we have a valid verified ID service
    // TODO: Implement downloadManifest method whenever we have a valid verified ID service
}
