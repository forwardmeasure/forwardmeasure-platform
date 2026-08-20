package com.forwardmeasure.build.contract;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class SpringBomTest {
  @Test
  void resolvesSpringFromOverlay() {
    assertNotNull(ApplicationContext.class);
  }
}
