package com.smoogiee.verifiedidbackend.service;

import com.smoogiee.verifiedidbackend.config.AppConfig;
import com.smoogiee.verifiedidbackend.config.VerifiedIdProperties;
import com.smoogiee.verifiedidbackend.exception.AuthenticationException;
import com.smoogiee.verifiedidbackend.model.verifiedid.*;
import com.smoogiee.verifiedidbackend.utils.ServerUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service class used for calls into Microsoft's Verified ID API
 */
@Slf4j
@Service
public class VerifiedIdService {
    private final CacheService<String, String> cacheService;
    private final MSALService msalService;
    private final VerifiedIdProperties verifiedIdProperties;
    private final SecureRandom secureRandom;

    /**
     * Constructor
     *
     * @param cacheService Service bean used for caching MSAL access token
     * @param msalService Service bean used for accessing MSAL library
     * @param verifiedIdProperties Property bean containing Verified ID configuration properties
     */
    @Autowired
    public VerifiedIdService(CacheService<String, String> cacheService,
                             MSALService msalService,
                             VerifiedIdProperties verifiedIdProperties) {
        this.cacheService = cacheService;
        this.msalService = msalService;
        this.verifiedIdProperties = verifiedIdProperties;
        this.secureRandom = new SecureRandom(); // Defaults to SHA1PRNG Algorithm
    }

    /**
     * Initiate the issuance of a Verified ID
     *
     * @param payload IssuanceRequest object
     * @return A JSON encoded string containing the Verified ID API response
     * @throws AuthenticationException When MSAL service fails to obtain an access token
     */
    public String initiateIssuanceRequest(IssuanceRequest payload) throws AuthenticationException {
        // Retrieve MSAL access token from the cache service
        // Obtains a MSAL access token from Microsoft if token not found in the cache
        String accessToken = retrieveAccessToken();

        // Prepare endpoint string
        String apiEndpoint = verifiedIdProperties.getApiEndpoint() + "verifiableCredentials/createIssuanceRequest";

        // Create and execute WebClient call to Verified ID API endpoint
        WebClient client = WebClient.create();
        WebClient.ResponseSpec responseSpec = client
                .post()
                .uri(apiEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(payload))
                .retrieve();

        // Retrieve response and return
        // NOTE: This blocks the reactive nature of WebFlux. This is OK because endpoints are blocking MVC.
        //       If endpoints must be reactive, this should return a WebFlux object.
        return responseSpec.bodyToMono(String.class).block();
    }

    /**
     * Initiate the presentation of a Verified ID
     *
     * @param payload PresentationRequest object
     * @return A JSON encoded string containing the Verified ID API response
     * @throws AuthenticationException When MSAL service fails to obtain an access token
     */
    public String initiatePresentationRequest(PresentationRequest payload) throws AuthenticationException {
        // Retrieve MSAL access token from the cache service
        // Obtains a MSAL access token from Microsoft if token not found in the cache
        String accessToken = retrieveAccessToken();

        // Prepare endpoint string
        String apiEndpoint = verifiedIdProperties.getApiEndpoint() + "verifiableCredentials/createPresentationRequest";

        // Create and execute WebClient call to Verified ID API endpoint
        WebClient client = WebClient.create();
        WebClient.ResponseSpec responseSpec = client
                .post()
                .uri(apiEndpoint)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(payload))
                .retrieve();

        // Retrieve response and return
        // NOTE: This blocks the reactive nature of WebFlux. This is OK because endpoints are blocking MVC.
        //       If endpoints must be reactive, this should return a WebFlux object.
        return responseSpec.bodyToMono(String.class).block();
    }

    public IssuanceRequest createIssuanceRequest(HttpServletRequest httpServletRequest, Claims claims) {
        IssuanceRequest request = new IssuanceRequest();

        request.setRegistration(new Registration());
        request.setAuthority(verifiedIdProperties.getDidAuthority());
        request.setIncludeReceipt(true);
        request.getRegistration().setClientName(verifiedIdProperties.getClientName());

        request.setCallback(new Callback());
        request.getCallback().setUrl(ServerUtils.getBasePath(httpServletRequest) + "api/issuer/callback");
        request.getCallback().setState(UUID.randomUUID().toString());
        request.getCallback().setHeaders(new Headers());
        // TODO: Securely generate API key
        request.getCallback().getHeaders().setApiKey(verifiedIdProperties.getApiKey());

        request.setType(verifiedIdProperties.getCredentialType());
        request.setManifest(verifiedIdProperties.getManifestUrl());

        request.setClaims(claims);

        if (!ServerUtils.fromMobile(httpServletRequest)) {
            int pinCodeLength = verifiedIdProperties.getPinCodeLength();
            if (pinCodeLength > 0) {
                request.setPin(new Pin());
                request.getPin().setLength(pinCodeLength);
                request.getPin().setValue(generatePinCode(pinCodeLength));
            }
        }

        return request;
    }

    public PresentationRequest createPresentationRequest(HttpServletRequest httpServletRequest) {
        PresentationRequest request = new PresentationRequest();

        request.setRegistration(new Registration());
        request.setAuthority(verifiedIdProperties.getDidAuthority());
        request.setIncludeReceipt(true);
        request.getRegistration().setClientName(verifiedIdProperties.getClientName());

        request.setCallback(new Callback());
        request.getCallback().setUrl(ServerUtils.getBasePath(httpServletRequest) + "api/verifier/callback");
        request.getCallback().setState(UUID.randomUUID().toString());
        request.getCallback().setHeaders(new Headers());
        request.getCallback().getHeaders().setApiKey(verifiedIdProperties.getApiKey());

        request.setRequestedCredentials(new ArrayList<>());
        RequestedCredential requestedCredential = new RequestedCredential();
        requestedCredential.setType(verifiedIdProperties.getCredentialType());
        requestedCredential.setPurpose(verifiedIdProperties.getPurpose());
        requestedCredential.setAcceptedIssuers(new ArrayList<>());
        requestedCredential.getAcceptedIssuers().add(verifiedIdProperties.getDidAuthority());
        requestedCredential.setConfiguration(new Configuration());
        requestedCredential.getConfiguration().setValidation(new Validation());
        requestedCredential.getConfiguration().getValidation().setAllowRevoked(false);
        requestedCredential.getConfiguration().getValidation().setValidateLinkedDomain(true);
        request.getRequestedCredentials().add(requestedCredential);

        return request;
    }

    private String generatePinCode(int length) {
        int min = 0;
        int max = Integer.parseInt("999999999999999999999".substring(0, length));
        int pin = secureRandom.nextInt(min, max);
        return String.format(String.format("%%0%dd", length), pin);
    }

    private String retrieveAccessToken() throws AuthenticationException {
        try {
            String accessToken = cacheService.getIfPresent("MSALAccessToken");
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = msalService.getAccessToken();
                cacheService.put("MSALAccessToken", accessToken);
            }
            return accessToken;
        } catch (Exception ex) {
            throw new AuthenticationException(ex.getMessage());
        }
    }
}
