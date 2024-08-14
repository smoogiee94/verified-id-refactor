package com.smoogiee.verifiedidbackend.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Server util class
 */
public class ServerUtils {
    /**
     * Gets a HTTPS protocol URL using a provided HttpServletRequest
     *
     * @param request The HTTP request to use for generating the base path
     * @return The HTTPS base path for the server
     */
    public static String getBasePath(HttpServletRequest request) {
        return "https://" + request.getServerName() + ":" + request.getServerPort() + "/";
    }

    public static String decodeBase64(String base64String) {
        // Pad string up to base64 if needed
        if (base64String.length() % 4 > 0) {
            base64String += "====".substring(base64String.length() % 4);
        }
        return new String(Base64.getUrlDecoder().decode(base64String), StandardCharsets.UTF_8);
    }

    /**
     * Checks whether a request originates from a mobile browser
     * @param request The HTTP request to check
     * @return true if the request originates from a mobile browser. false otherwise
     */
    public static boolean fromMobile(HttpServletRequest request) {
        String userAgent = Optional
                .ofNullable(request.getHeader(HttpHeaders.USER_AGENT))
                .orElse("")
                .toLowerCase(Locale.ROOT);
        return userAgent.contains("android") || userAgent.contains("iphone");
    }
}
