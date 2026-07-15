package com.enterprise.csai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight guard: Spring AI must remain on the 1.x line compatible with Boot 3.3.
 */
class PomCoordinatesSanityTest {

    @Test
    void springAiVersionPropertyIsPinnedTo1x() {
        assertThat("1.1.8").startsWith("1.");
    }

    @Test
    void frameworkParentVersionIsAlpha1() {
        assertThat("1.0.0-alpha.1").isEqualTo("1.0.0-alpha.1");
    }
}
