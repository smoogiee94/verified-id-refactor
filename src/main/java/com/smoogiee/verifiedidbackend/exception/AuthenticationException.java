package com.smoogiee.verifiedidbackend.exception;

/**
 * Exception used whenever VerifiedIdService encounters
 * an error retrieving a MSAL access token
 */
public class AuthenticationException extends Exception {
    public AuthenticationException(String errorMessage) {
        super(errorMessage);
    }
}
