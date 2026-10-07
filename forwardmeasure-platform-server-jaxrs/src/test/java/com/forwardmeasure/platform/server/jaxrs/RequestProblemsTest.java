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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestProblemsTest {
  @Test
  void normalizesKnownUnknownAndMissingHttpReasonPhrases() {
    assertEquals("Not Found", RequestProblems.httpError(404).getTitle());
    assertEquals("HTTP 499", RequestProblems.httpError(499).getTitle());
    assertEquals("HTTP 499", RequestProblems.httpError(499).getDetail());
    assertEquals("HTTP 400", RequestProblems.httpError(400, null, null).getDetail());
    assertEquals("HTTP 400", RequestProblems.httpError(400, " ", null).getTitle());
    assertEquals(
        "client detail",
        RequestProblems.httpError(400, "Bad Request", "client detail").getDetail());
  }

  @Test
  void boundsScalarEchoesAndNeverSerializesAnObjectOrRequestBody() {
    for (Object scalar :
        List.of(
            "text",
            42,
            true,
            java.time.DayOfWeek.MONDAY,
            UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"),
            Instant.parse("2026-10-07T00:00:00Z"))) {
      assertEquals(scalar.toString(), RequestProblems.rejectedValue(scalar));
    }
    assertEquals("x".repeat(256), RequestProblems.rejectedValue("x".repeat(257)));
    assertEquals("x".repeat(256), RequestProblems.rejectedValue("x".repeat(256)));
    assertNull(RequestProblems.rejectedValue(null));
    assertNull(
        RequestProblems.rejectedValue(
            new Object() {
              public String toString() {
                throw new AssertionError("request object must not be echoed");
              }
            }));
  }

  @Test
  void sortsViolationsByFieldThenMessageWithoutMutatingTheInput() {
    var violations =
        List.of(
            RequestProblems.violation("z", "a", null),
            RequestProblems.violation("a", "z", null),
            RequestProblems.violation("a", "a", null));
    var problem = RequestProblems.badRequest(violations);
    assertEquals(
        List.of("a:a", "a:z", "z:a"),
        problem.getViolations().stream().map(v -> v.getField() + ":" + v.getMessage()).toList());
    assertEquals("z", violations.getFirst().getField());
    assertTrue(problem.getStatus() == 400);
  }
}
