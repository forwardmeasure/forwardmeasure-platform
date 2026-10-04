/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at https://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package com.forwardmeasure.platform.server.jaxrs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * {@link QueryParameterConversionFilter}'s conversions, each the way the runtimes convert the type.
 * The services' API contract tests drive the filter over HTTP on all three frameworks.
 */
final class QueryParameterConversionFilterTest {

  /** Shaped like an OpenAPI-generated enum: lowercase wire values, converted by fromValue. */
  enum GeneratedStatus {
    IN_REVIEW("in_review"),
    PUBLISHED("published");

    private final String value;

    GeneratedStatus(String value) {
      this.value = value;
    }

    public static GeneratedStatus fromValue(String value) {
      for (GeneratedStatus status : values()) {
        if (status.value.equals(value)) {
          return status;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @Test
  void numbersConvertOnlyWhenTheyAreNumbersOfTheirSize() {
    assertTrue(QueryParameterConversionFilter.converts(Integer.class, "32"));
    assertTrue(QueryParameterConversionFilter.converts(int.class, "-1"));
    assertFalse(QueryParameterConversionFilter.converts(Integer.class, "many"));
    assertFalse(QueryParameterConversionFilter.converts(int.class, "4294967296"));
    assertTrue(QueryParameterConversionFilter.converts(Long.class, "4294967296"));
    assertFalse(QueryParameterConversionFilter.converts(long.class, "1.5"));
  }

  @Test
  void aUuidConvertsOnlyWhenItIsOne() {
    assertTrue(QueryParameterConversionFilter.converts(UUID.class, UUID.randomUUID().toString()));
    assertFalse(QueryParameterConversionFilter.converts(UUID.class, "not-a-uuid"));
  }

  @Test
  void aGeneratedEnumConvertsFromItsWireValues() {
    assertTrue(QueryParameterConversionFilter.converts(GeneratedStatus.class, "in_review"));
    assertFalse(QueryParameterConversionFilter.converts(GeneratedStatus.class, "IN_REVIEW"));
    assertFalse(QueryParameterConversionFilter.converts(GeneratedStatus.class, "retired"));
  }

  @Test
  void aPlainEnumConvertsFromItsConstantNames() {
    assertTrue(QueryParameterConversionFilter.converts(DayOfWeek.class, "MONDAY"));
    assertFalse(QueryParameterConversionFilter.converts(DayOfWeek.class, "monday"));
  }

  /** A date-time is any RFC 3339 timestamp: any offset, any fraction of a second, or none. */
  @Test
  void aDateTimeConvertsOnlyFromRfc3339() {
    for (Class<?> type : new Class<?>[] {Date.class, OffsetDateTime.class}) {
      assertTrue(QueryParameterConversionFilter.converts(type, "2026-10-03T12:00:00Z"));
      assertTrue(QueryParameterConversionFilter.converts(type, "2026-10-03T12:00:00.123456+02:00"));
      assertFalse(QueryParameterConversionFilter.converts(type, "2026-10-03"));
      assertFalse(QueryParameterConversionFilter.converts(type, "yesterday"));
    }
  }

  @Test
  void anyOtherTypeIsLeftToTheRuntime() {
    assertTrue(QueryParameterConversionFilter.converts(String.class, "anything"));
  }
}
