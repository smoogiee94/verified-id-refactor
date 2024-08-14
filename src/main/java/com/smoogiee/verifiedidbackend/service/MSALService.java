package com.smoogiee.verifiedidbackend.service;

import com.microsoft.aad.msal4j.*;
import com.smoogiee.verifiedidbackend.config.AzureProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Service class used for calls into Microsoft's MSAL library
 */
@Slf4j
@Service
public class MSALService {
    private final AzureProperties azureProperties;

    /**
     * Constructor
     *
     * @param azureProperties Property bean containing Azure configuration properties
     */
    @Autowired
    public MSALService(AzureProperties azureProperties) {
        this.azureProperties = azureProperties;
    }

    /**
     * Gets an MSAL access token
     *
     * @return A String object containing the MSAL access token
     * @throws Exception When an error occurs during authentication
     */
    public String getAccessToken() throws Exception {
        // Retrieve values important for authentication flow
        boolean managedIdentity = azureProperties.isManagedId();
        String scope = azureProperties.getScope();

        // Check if identity is managed through ManagedIdentity
        // If managed, authenticate using ManagedIdentity
        if (managedIdentity) {
            log.debug("MSAL Acquire AccessToken via Managed Identity");
            ManagedIdentityId managedIdentityId = ManagedIdentityId.systemAssigned();
            ManagedIdentityApplication msiApp = ManagedIdentityApplication
                    .builder(managedIdentityId)
                    .logPii(false)
                    .build();
            IAuthenticationResult result = msiApp
                    .acquireTokenForManagedIdentity(ManagedIdentityParameters
                            .builder(scope)
                            .build())
                    .get();
            return result.accessToken();
        }

        // If not managed through ManagedIdentity,
        // retrieve values necessary to check if
        // authenticate should be performed using
        // client ID/secret pair
        String authority = azureProperties.getAuthority();
        ConfidentialClientApplication app;
        String clientId = azureProperties.getClientId();
        String clientSecret = azureProperties.getClientSecret();
        if (!clientSecret.isEmpty()) {
            log.debug("MSAL Acquire AccessToken via Client Credentials");

            // TODO: Delete below stub and uncomment code
            return "1234";
//            app = ConfidentialClientApplication
//                    .builder(
//                            clientId,
//                            ClientCredentialFactory.createFromSecret(clientSecret))
//                    .authority(authority)
//                    .build();
        } else {
            // Otherwise, attempt to authenticate using
            // client private/public key pair
            log.debug("MSAL Acquire AccessToken via Certificate");
            String certLocation = azureProperties.getClientCertLocation();
            String certKeyLocation = azureProperties.getClientCertKey();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(certKeyLocation)));
            PrivateKey key = KeyFactory
                    .getInstance("RSA")
                    .generatePrivate(spec);
            InputStream certStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(certLocation)));
            X509Certificate cert = (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(certStream);
            app = ConfidentialClientApplication
                    .builder(
                            clientId,
                            ClientCredentialFactory.createFromCertificate(key, cert))
                    .authority(authority)
                    .build();
        }

        // Execute authentication request
        // for client/secret and private/public key pair flows
        // and return access token
        ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters
                .builder(Collections.singleton(scope))
                .build();
        CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParameters);
        IAuthenticationResult result = future.get();
        return result.accessToken();
    }
}
