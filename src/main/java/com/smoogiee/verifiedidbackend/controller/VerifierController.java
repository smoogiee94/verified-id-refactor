package com.smoogiee.verifiedidbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smoogiee.verifiedidbackend.config.AppConfig;
import com.smoogiee.verifiedidbackend.config.VerifiedIdProperties;
import com.smoogiee.verifiedidbackend.exception.AuthenticationException;
import com.smoogiee.verifiedidbackend.model.verifiedid.ApiResponse;
import com.smoogiee.verifiedidbackend.model.verifiedid.FaceCheck;
import com.smoogiee.verifiedidbackend.model.verifiedid.PresentationRequest;
import com.smoogiee.verifiedidbackend.service.CacheService;
import com.smoogiee.verifiedidbackend.service.VerifiedIdService;
import com.smoogiee.verifiedidbackend.utils.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller class used for verifying Verified IDs
 */
@Slf4j
@RestController
@EnableCaching
public class VerifierController {
    private final ObjectMapper objectMapper;
    private final CacheService<String, String> cacheService;
    private final VerifiedIdService verifiedIdService;
    private final VerifiedIdProperties verifiedIdProperties;

    /**
     * Constructor
     *
     * @param objectMapper A fasterXML Jackson ObjectMapper used for marshaling JSON objects
     * @param cacheService A cache service with strings as key:value pairs
     * @param verifiedIdService A VerifiedIdService bean used to make calls into Microsoft Verified ID
     */
    @Autowired
    public VerifierController(ObjectMapper objectMapper,
                              CacheService<String, String> cacheService,
                              VerifiedIdService verifiedIdService,
                              VerifiedIdProperties verifiedIdProperties) {
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.verifiedIdService = verifiedIdService;
        this.verifiedIdProperties = verifiedIdProperties;
    }

    @PostMapping(
            path = "/api/verifier/request",
            consumes = "applciation/json",
            produces = "application/json"
    )
    public ResponseEntity<String> presentationRequest(HttpServletRequest request, @RequestHeader HttpHeaders headers) {
        //Log the request
        LogUtils.logHttpRequest(request);

        // Prepare response body
        String responseBody;

        try {
            // Create Presentation Request
            PresentationRequest presentationRequest = verifiedIdService.createPresentationRequest(request);
            String correlationId = presentationRequest.getCallback().getState();
            String faceCheck  = request.getParameter("faceCheck");
            if (faceCheck.equals("1")) {
                String photoClaimName = request.getParameter("photoClaimName");
                if (photoClaimName.isBlank()) {
                    photoClaimName = verifiedIdProperties.getPhotoClaimName();
                }
                FaceCheck fc = new FaceCheck();
                fc.setSourcePhotoClaimName(photoClaimName);
                fc.setMatchConfidenceThreshold(70);
                presentationRequest
                        .getRequestedCredentials()
                        .get(0)
                        .getConfiguration()
                        .getValidation()
                        .setFaceCheck(fc);
            }
            String payload = objectMapper
                    .writer()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(presentationRequest);

            // Print debug payload
            log.debug(payload);

            // Cache request correlation id and associated status
            // This is required for when Microsoft issues the presentation callback
            ObjectNode data = objectMapper.createObjectNode();
            data.put("status", "request_created");
            data.put("message", "Waiting for QR code to be scanned");
            String cachedData = objectMapper
                    .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(data);
            cacheService.put(correlationId, cachedData);

            // Call Verified ID API
            responseBody = verifiedIdService.initiatePresentationRequest(presentationRequest);

            // Marshal Verified ID API response
            ApiResponse presentationResponse = objectMapper.readValue(responseBody, ApiResponse.class);
            presentationResponse.setId(correlationId);
            responseBody = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(presentationResponse);

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
                    .body("Internal authentication failed,.");
        }

        // Set up and return response
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(responseBody);
    }

    @GetMapping(
            path = "/api/verifier/get-presentation-details",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<String> getPresentationDetails(HttpServletRequest request, @RequestHeader HttpHeaders headers) {
        LogUtils.logHttpRequest(request);
        String responseBody = "";
        try {
            PresentationRequest presentationRequest = verifiedIdService.createPresentationRequest(request);
            ObjectNode data = objectMapper.createObjectNode();
            data.put("clientName", presentationRequest.getRegistration().getClientName());
            data.put("purpose", presentationRequest.getRequestedCredentials().get(0).getPurpose());
            data.put("didAuthority", presentationRequest.getAuthority());
            data.put("type", presentationRequest.getRequestedCredentials().get(0).getType());
            data.put("acceptedIssuers", presentationRequest.getRequestedCredentials().get(0).getAcceptedIssuers().get(0));
            data.put("photoClaimName", verifiedIdProperties.getPhotoClaimName());
            data.put("useFaceCheck", verifiedIdProperties.getUseFaceCheck());
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Request may be malformed.");
        }

        // Set up and return response
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(responseBody);
    }
}
