package com.forwardmeasure.platform.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlatformTenantHostTest {

    @Test
    void canonicalizesOnlyAnExactHttpHost() {
        assertEquals("tenant.example.com",
                PlatformTenantContextResource.canonicalHost(
                        "TENANT.Example.Com.:8443"));
        assertThrows(IllegalArgumentException.class,
                () -> PlatformTenantContextResource.canonicalHost(""));
        assertThrows(IllegalArgumentException.class,
                () -> PlatformTenantContextResource.canonicalHost("bad host"));
    }
}
