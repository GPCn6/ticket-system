package com.jsu.common.security;

import jakarta.servlet.http.HttpServletRequest;

/** Validates the hop from the gateway to a servlet service. */
public final class InternalAuth {
    public static final String HEADER = "X-Gateway-Auth";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private InternalAuth() {
    }

    public static boolean isTrusted(HttpServletRequest request, String expectedSecret) {
        return expectedSecret != null && !expectedSecret.isBlank()
                && expectedSecret.equals(request.getHeader(HEADER));
    }
}
