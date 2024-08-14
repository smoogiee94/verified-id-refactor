package com.smoogiee.verifiedidbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smoogiee.verifiedidbackend.config.AppConfig;
import com.smoogiee.verifiedidbackend.config.VerifiedIdProperties;
import com.smoogiee.verifiedidbackend.model.verifiedid.CallbackError;
import com.smoogiee.verifiedidbackend.model.verifiedid.CallbackEvent;
import com.smoogiee.verifiedidbackend.service.CacheService;
import com.smoogiee.verifiedidbackend.service.VerifiedIdService;
import com.smoogiee.verifiedidbackend.utils.LogUtils;
import com.smoogiee.verifiedidbackend.utils.ServerUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller class used for handling callback calls
 */
@Slf4j
@RestController
@EnableCaching
public class CallbackController {
    private final ObjectMapper objectMapper;
    private final CacheService<String, String> cacheService;
    private final VerifiedIdProperties verifiedIdProperties;

    @Autowired
    public CallbackController(ObjectMapper objectMapper,
                              CacheService<String, String> cacheService,
                              VerifiedIdProperties verifiedIdProperties) {
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.verifiedIdProperties = verifiedIdProperties;
    }

    /**
     * Private handler helper method. This is used by the callback endpoints.
     *
     * @param request The request sent to the callback endpoint
     * @param headers The headers sent to the callback endpoint
     * @param body The body sent to the callback endpoint
     * @param requestType The request type set by the callback endpoint
     * @return A ResponseEntity object representing the Verified ID API callback response
     */
    private ResponseEntity<String> handleRequestCallback(HttpServletRequest request,
                                                         @RequestHeader HttpHeaders headers,
                                                         @RequestBody String body,
                                                         String requestType) {
        LogUtils.logHttpRequest(request);
        try {
            // TODO: Securely validate API key
            if (!request.getHeader("api-key").equals(verifiedIdProperties.getApiKey())) {
                log.error("api-key wrong or missing");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("api-key wrong or missing");
            }

            // Marshal callback event from Microsoft
            CallbackEvent callbackEvent;
            try {
                callbackEvent = objectMapper.readValue(body, CallbackEvent.class);
            } catch (IOException ex) {
                log.error(ex.getMessage());
                JsonNode cb = objectMapper.readTree(body);
                callbackEvent = new CallbackEvent();
                callbackEvent.setRequestStatus(cb.path("requestStatus").asText());
                callbackEvent.setState(cb.path("state").asText());
            }

            // Initialize a list of valid issuance statuses
            List<String> issuanceStatus = new ArrayList<>() {{
                add("request_retrieved");
                add("issuance_successful");
                add("issuance_error");
            }};

            // Initialize a list of valid presentation statuses
            List<String> presentationStatus = new ArrayList<>() {{
                add("request_retrieved");
                add("presentation_verified");
                add("presentation_error");
            }};

            // Initialize a list of valid selfie statuses
            List<String> selfieStatus = new ArrayList<>() {{
                add("selfie_taken");
            }};

            // Handle actual callback event
            if ((requestType.equals("issuance") && issuanceStatus.contains(callbackEvent.getRequestStatus())) ||
                (requestType.equals("presentation") && presentationStatus.contains(callbackEvent.getRequestStatus())) ||
                (requestType.equals("selfie") && selfieStatus.contains(callbackEvent.getRequestStatus()))) {
                String data = cacheService.getIfPresent(callbackEvent.getState());
                if (data == null) {
                    log.info("Unknown state: " + callbackEvent.getState());
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("Unknown state");
                } else {
                    JsonNode cachedData = objectMapper.readTree(data);
                    ((ObjectNode) cachedData).put("status", callbackEvent.getRequestStatus());
                    ((ObjectNode) cachedData).put("callback", body);
                    cacheService.put(callbackEvent.getState(),
                            objectMapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(cachedData));
                }
            } else {
                    log.error("Unsupported requestStatus: " + callbackEvent.getRequestStatus());
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("Unsupported requestStatus: " + callbackEvent.getRequestStatus());
            }
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Technical error");
        }
        return ResponseEntity
                .ok()
                .body("{}");
    }

    /**
     * Issue request callback endpoint. This endpoint is called by Microsoft during the issuance process.
     *
     * @param request The request sent by Microsoft
     * @param headers The request headers sent by Microsoft
     * @param body The request body sent by Microsoft
     * @return A ResponseEntity object representing the Verified ID API callback response
     */
    @PostMapping(
            path = "/api/issuer/callback",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<String> issueRequestCallback(HttpServletRequest request,
                                                       @RequestHeader HttpHeaders headers,
                                                       @RequestBody String body) {
        return handleRequestCallback(request, headers, body, "issuance");
    }

    /**
     * Verify request callback endpoint. This endpoint is called by Microsoft during the verification process.
     *
     * @param request The request set by Microsoft
     * @param headers The Request headers sent by Microsoft
     * @param body The request body sent by Microsoft
     * @return A ResponseENtity object representing the Verified ID API callback response
     */
    @PostMapping(
            path = "/api/verifier/callback",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<String> verifyRequestCallback(HttpServletRequest request,
                                                        @RequestHeader HttpHeaders headers,
                                                        @RequestBody String body) {
        return handleRequestCallback(request, headers, body, "presentation");
    }

    @GetMapping(
            path = "/api/status",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<String> requestStatus(HttpServletRequest request,
                                                @RequestHeader HttpHeaders headers,
                                                @RequestParam String id) {
        LogUtils.logHttpRequest(request);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        String responseBody = "{'status': 'request_not_created', 'message': 'No data'}";
        try {
            String cachedData = cacheService.getIfPresent(id);
            if (cachedData == null || cachedData.isEmpty()) {
                return ResponseEntity
                        .ok()
                        .headers(responseHeaders)
                        .body(responseBody);
            }
            JsonNode cacheData = objectMapper.readTree(cachedData);
            String requestStatus = cacheData.path("status").asText();

            ObjectNode statusResponse = objectMapper.createObjectNode();
            statusResponse.put("status", requestStatus);

            CallbackEvent callbackEvent = null;
            String callBack = cacheData.path("callback").asText();
            if (callBack != null && !callBack.isEmpty()) {
                try {
                    callbackEvent = objectMapper.readValue(callBack, CallbackEvent.class);
                } catch (IOException ex) {
                    JsonNode cb = objectMapper.readTree(callBack);
                    callbackEvent = new CallbackEvent();
                    callbackEvent.setRequestId(cb.path("requestId").asText());
                    callbackEvent.setRequestStatus(cb.path("requestStatus").asText());
                    callbackEvent.setState(cb.path("state").asText());
                    callbackEvent.setError(new CallbackError());
                    callbackEvent.getError().setCode(cb.path("error").path("code").asText());
                    callbackEvent.getError().setMessage(cb.path("error").path("message").asText());
                }
            }

            if (callbackEvent == null) {
                log.error("callbackEvent was unexpectedly null");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
            }

            switch (requestStatus) {
                case "request_created" -> statusResponse
                        .put("message", "Waiting to scan QR code");
                case "request_retrieved" -> statusResponse
                        .put("message", "QR code is scanned. Waiting for user action...");
                case "issuance_error" -> statusResponse
                        .put("message", "Issuance failed: " + callbackEvent.getError().getMessage());
                case "issuance_successful" -> statusResponse
                        .put("message", "Issuance successful");
                case "presentation_error" -> statusResponse
                        .put("message", "Presentation failed: " + callbackEvent.getError().getMessage());
                case "presentation_verified" -> {
                    statusResponse.put("subject", callbackEvent.getSubject());
                    String vcData = objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(callbackEvent.getVerifiedCredentialsData());
                    statusResponse.set("payload", objectMapper.readTree(vcData));
                    statusResponse.set("type", objectMapper.valueToTree(
                            callbackEvent
                                    .getVerifiedCredentialsData()
                                    .get(0)
                                    .getType()
                    ));
                    statusResponse.put("issuanceDate",
                            callbackEvent
                                    .getVerifiedCredentialsData()
                                    .get(0)
                                    .getIssuanceDate()
                    );
                    statusResponse.put("expirationDate",
                            callbackEvent
                                    .getVerifiedCredentialsData()
                                    .get(0)
                                    .getExpirationDate()
                    );
                    if (callbackEvent.getReceipt() != null && callbackEvent.getReceipt().getVp_token() != null) {
                        String vp = ServerUtils.decodeBase64(
                                callbackEvent
                                        .getReceipt()
                                        .getVp_token()
                                        .split("\\.")[1]
                        );
                        JsonNode vpToken = objectMapper.readTree(vp);
                        String vc = ServerUtils.decodeBase64(
                                vpToken
                                        .path("vp")
                                        .path("verifiableCredential")
                                        .get(0)
                                        .asText()
                                        .split("\\.")[1]
                        );
                        JsonNode vcToken = objectMapper.readTree(vc);
                        statusResponse.put("jti", vcToken.path("jti").asText());
                    }
                }
            }
            responseBody = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(statusResponse);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Technical error");
        }
        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(responseBody);
    }

    @GetMapping(
            path = "/api/cache",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<String> verifierCache(HttpServletRequest request,
                                                @RequestHeader HttpHeaders headers,
                                                @RequestParam String id) {
        LogUtils.logHttpRequest(request);
        String responseBody = cacheService.getIfPresent(id);
        if (responseBody == null) {
            responseBody = "is null";
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        return ResponseEntity
                .ok()
                .headers(responseHeaders)
                .body(responseBody);
    }
}
