package com.forwardmeasure.build.contract;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.runtime.LaunchMode;
import org.junit.jupiter.api.Test;

class QuarkusBomTest {
  @Test
  void resolvesQuarkusFromOverlay() {
    assertNotNull(LaunchMode.class);
  }
}
