package com.smoogiee.verifiedidbackend.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Logging util class
 */
@Slf4j
public class LogUtils {
    /**
     * Logs the HTTP method and request URL
     *
     * @param request The HTTP request to log
     */
    public static void logHttpRequest(HttpServletRequest request) {
        log.info("{} - {}{}",
                request.getMethod(),
                request.getRequestURL().toString(),
                request.getQueryString() != null ? "?" + request.getQueryString() : "");
    }
}
