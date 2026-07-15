package com.enterprise.csai.security;

import java.util.List;
import java.util.Set;

public record ApiKeyPrincipal(
        String id,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        String want = role.startsWith("ROLE_") ? role.substring(5) : role;
        return roles.contains(want) || roles.contains("ROLE_" + want);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static ApiKeyPrincipal anonymous() {
        return new ApiKeyPrincipal("anonymous", Set.of());
    }

    public static ApiKeyPrincipal system() {
        return new ApiKeyPrincipal("system", Set.of("ADMIN", "CLIENT"));
    }

    public List<String> roleList() {
        return roles == null ? List.of() : List.copyOf(roles);
    }
}
