package com.enterprise.csai.modelgateway;

/**
 * Public model descriptor without secrets.
 */
public record ModelView(
        String id,
        String displayName,
        String modelName,
        ModelRole role,
        boolean enabled
) {
}
