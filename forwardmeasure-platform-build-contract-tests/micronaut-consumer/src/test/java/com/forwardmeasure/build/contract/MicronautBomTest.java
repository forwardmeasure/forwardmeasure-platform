package com.forwardmeasure.build.contract;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

class MicronautBomTest {
  @Test
  void resolvesMicronautFromOverlay() {
    assertNotNull(ApplicationContext.class);
  }
}
