package com.forwardmeasure.platform.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlatformApplicationPathTest {

    @Test
    void retainsValidSameOriginApplicationPaths() {
        assertEquals("/workflows/",
                PlatformApplicationPath.parse("/workflows/").value());
        assertEquals("/ei/",
                PlatformApplicationPath.parse("/ei/").value());
    }

    @Test
    void rejectsExternalAmbiguousAndNonCanonicalTargets() {
        for (String candidate : new String[]{
                "https://outside.example/workflows",
                "//outside.example/workflows",
                "/\\outside.example/workflows",
                "/workflows?tenant=other",
                "/workflows#draft",
                "/workflows/../admin",
                " workflows"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> PlatformApplicationPath.parse(candidate), candidate);
        }
    }
}
