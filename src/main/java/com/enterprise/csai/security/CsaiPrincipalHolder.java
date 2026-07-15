package com.enterprise.csai.security;

/**
 * Request-scoped principal holder for CSAI API keys.
 */
public final class CsaiPrincipalHolder {

    private static final ThreadLocal<ApiKeyPrincipal> HOLDER = new ThreadLocal<>();

    private CsaiPrincipalHolder() {
    }

    public static void set(ApiKeyPrincipal principal) {
        HOLDER.set(principal);
    }

    public static ApiKeyPrincipal get() {
        ApiKeyPrincipal p = HOLDER.get();
        return p != null ? p : ApiKeyPrincipal.anonymous();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
