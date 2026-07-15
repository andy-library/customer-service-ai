package com.enterprise.csai.common.error;

import com.microservice.framework.common.error.FrameworkErrorCode;

/**
 * Customer-service AI domain error codes (module CSAI).
 */
public final class CsaiErrorCodes {

    public static final FrameworkErrorCode MODEL_NOT_FOUND = FrameworkErrorCode.of("CSAI", "BIZ", 1);
    public static final FrameworkErrorCode MODEL_INVOKE_FAILED = FrameworkErrorCode.of("CSAI", "BIZ", 2);
    public static final FrameworkErrorCode DOCUMENT_NOT_FOUND = FrameworkErrorCode.of("CSAI", "BIZ", 3);
    public static final FrameworkErrorCode DOCUMENT_UNSUPPORTED = FrameworkErrorCode.of("CSAI", "BIZ", 4);
    public static final FrameworkErrorCode DOCUMENT_TOO_LARGE = FrameworkErrorCode.of("CSAI", "BIZ", 5);
    public static final FrameworkErrorCode DOCUMENT_INGEST_FAILED = FrameworkErrorCode.of("CSAI", "BIZ", 6);
    public static final FrameworkErrorCode CHAT_FAILED = FrameworkErrorCode.of("CSAI", "BIZ", 7);
    public static final FrameworkErrorCode SESSION_FORBIDDEN = FrameworkErrorCode.of("CSAI", "BIZ", 8);
    public static final FrameworkErrorCode SESSION_NOT_FOUND = FrameworkErrorCode.of("CSAI", "BIZ", 9);
    public static final FrameworkErrorCode KNOWLEDGE_FAILED = FrameworkErrorCode.of("CSAI", "BIZ", 10);
    public static final FrameworkErrorCode MODEL_TIMEOUT = FrameworkErrorCode.of("CSAI", "BIZ", 11);
    public static final FrameworkErrorCode INPUT_REJECTED = FrameworkErrorCode.of("CSAI", "BIZ", 12);

    private CsaiErrorCodes() {
    }
}
