package com.forwardmeasure.build.contract;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.quarkus.runtime.LaunchMode;
import org.junit.jupiter.api.Test;

class MultiFrameworkBomTest {
  @Test
  void resolvesAllFrameworkSurfacesWithTheCoreBaseline() {
    assertAll(
        () -> assertNotNull(ObjectMapper.class),
        () -> assertNotNull(LaunchMode.class),
        () -> assertNotNull(org.springframework.context.ApplicationContext.class),
        () -> assertNotNull(ApplicationContext.class));
  }
}
