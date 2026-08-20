package com.forwardmeasure.build.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlainConsumerTest {
  @Test
  void inheritsCoreDependenciesAndJunit() {
    assertEquals("com.fasterxml.jackson.databind.ObjectMapper", PlainConsumer.jacksonType());
  }
}
