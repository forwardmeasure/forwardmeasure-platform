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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.platform.server.jaxrs.ContractModels.Event;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * Real Jackson binding failures, with the mapper configured as Quarkus's and Spring Boot's server
 * mappers are (unknown properties ignored), mapped to the contract problem.
 */
class JsonBindingProblemsTest {
  private static final ObjectMapper SERVER_MAPPER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private static final String VALID_FIELDS =
      "\"eventId\":\"6f1c0c1e-7c1a-4f59-9d3a-2d0c1f0b2a11\",\"sequence\":1,"
          + "\"type\":\"STARTED\",\"occurredAt\":\"2026-09-30T00:00:00Z\"";
  private static final String EXECUTION_ID = "\"executionId\":{\"value\":\"e1\"}";

  @Test
  void aValidBodyBinds() {
    assertDoesNotThrow(() -> bind("{" + VALID_FIELDS + "," + EXECUTION_ID + "}"));
  }

  @Test
  void anUnknownFieldIsIgnoredAsOnEveryServer() {
    assertDoesNotThrow(() -> bind("{" + VALID_FIELDS + "," + EXECUTION_ID + ",\"extra\":1}"));
  }

  @Test
  void aMissingRequiredFieldIsNamedByItsJsonName() {
    assertViolation("{" + VALID_FIELDS + "}", "executionId", RequestProblems.REQUIRED);
  }

  @Test
  void aMissingNestedFieldIsNamedByItsPath() {
    assertViolation(
        "{" + VALID_FIELDS + ",\"executionId\":{}}", "executionId.value", RequestProblems.REQUIRED);
  }

  @Test
  void aMissingFieldInAListElementIsNamedByIndex() {
    String body =
        "{" + VALID_FIELDS + "," + EXECUTION_ID + ",\"steps\":[{\"display_name\":\"a\"},{}]}";

    assertViolation(body, "steps[1].display_name", RequestProblems.REQUIRED);
  }

  @Test
  void aValueOfTheWrongJsonTypeIsReported() {
    assertViolation(
        "{" + VALID_FIELDS + ",\"executionId\":\"x\"}", "executionId", RequestProblems.WRONG_TYPE);
    assertViolation(
        "{"
            + VALID_FIELDS.replace("\"sequence\":1", "\"sequence\":true")
            + ","
            + EXECUTION_ID
            + "}",
        "sequence",
        RequestProblems.WRONG_TYPE);
    assertViolation("[]", RequestProblems.BODY, RequestProblems.WRONG_TYPE);
  }

  @Test
  void aValueThatDoesNotParseIsAnInvalidValueWithoutEchoingIt() {
    String badUuid = VALID_FIELDS.replace("6f1c0c1e-7c1a-4f59-9d3a-2d0c1f0b2a11", "not-a-uuid");
    String badEnum = VALID_FIELDS.replace("\"STARTED\"", "\"BOGUS\"");
    String badDate = VALID_FIELDS.replace("2026-09-30T00:00:00Z", "yesterday");

    assertViolation(
        "{" + badUuid + "," + EXECUTION_ID + "}", "eventId", RequestProblems.INVALID_VALUE);
    assertViolation(
        "{" + badEnum + "," + EXECUTION_ID + "}", "type", RequestProblems.INVALID_VALUE);
    assertViolation(
        "{" + badDate + "," + EXECUTION_ID + "}", "occurredAt", RequestProblems.INVALID_VALUE);
  }

  @Test
  void aBodyThatIsNotJsonIsReportedAgainstTheBody() {
    assertViolation("{\"eventId\":", RequestProblems.BODY, RequestProblems.NOT_JSON);
    assertViolation("{\"eventId\" 1}", RequestProblems.BODY, RequestProblems.NOT_JSON);
  }

  @Test
  void anEmptyBodyIsAMissingBody() {
    assertViolation("", RequestProblems.BODY, RequestProblems.REQUIRED);
  }

  @Test
  void aModelJacksonCannotBindIsTheServersFaultNotTheClients() {
    JsonProcessingException failure =
        assertThrows(
            JsonProcessingException.class, () -> SERVER_MAPPER.readValue("{}", Unbindable.class));

    Problem problem = JsonBindingProblems.problem(failure);

    assertEquals(500, problem.getStatus());
    assertEquals("Internal Server Error", problem.getTitle());
  }

  @Test
  void aResponseJacksonCannotWriteIsTheServersFaultNotTheClients() {
    JsonProcessingException failure =
        assertThrows(
            JsonProcessingException.class,
            () -> SERVER_MAPPER.writeValueAsString(new Unwritable()));

    assertEquals(500, JsonBindingProblems.problem(failure).getStatus());
  }

  /** Two creators - Jackson can't choose, a fault in the model rather than the request. */
  static final class Unbindable {
    @com.fasterxml.jackson.annotation.JsonCreator
    Unbindable(@com.fasterxml.jackson.annotation.JsonProperty("a") String a) {}

    @com.fasterxml.jackson.annotation.JsonCreator
    Unbindable(@com.fasterxml.jackson.annotation.JsonProperty("b") Integer b) {}
  }

  static final class Unwritable {
    public String getValue() {
      throw new IllegalStateException("boom");
    }
  }

  /** A request model that isn't generated: a record that refuses a null argument by name. */
  record Launch(String correlationId, Spec spec) {
    Launch {
      Objects.requireNonNull(spec, "spec");
    }
  }

  record Spec(String name) {
    Spec {
      Objects.requireNonNull(name, "name");
    }
  }

  @Test
  void anArgumentARecordRefusesAsNullIsRequired() {
    assertRequired("{\"correlationId\":\"c\"}", "spec");
  }

  @Test
  void anArgumentANestedRecordRefusesAsNullIsNamedByItsPath() {
    assertRequired("{\"correlationId\":\"c\",\"spec\":{}}", "spec.name");
  }

  private static void assertRequired(String body, String field) {
    JsonProcessingException failure =
        assertThrows(
            JsonProcessingException.class, () -> SERVER_MAPPER.readValue(body, Launch.class));

    Problem problem = JsonBindingProblems.problem(failure);

    assertEquals(400, problem.getStatus(), failure::getMessage);
    assertEquals(1, problem.getViolations().size(), problem::toString);
    var violation = problem.getViolations().get(0);
    assertEquals(
        List.of(field, RequestProblems.REQUIRED),
        List.of(violation.getField(), violation.getMessage()));
  }

  private static void bind(String body) throws JsonProcessingException {
    SERVER_MAPPER.readValue(body, Event.class);
  }

  private static void assertViolation(String body, String field, String message) {
    JsonProcessingException failure = assertThrows(JsonProcessingException.class, () -> bind(body));

    Problem problem = JsonBindingProblems.problem(failure);

    assertEquals(400, problem.getStatus(), failure::getMessage);
    assertEquals("Bad Request", problem.getTitle());
    assertEquals(1, problem.getViolations().size(), problem::toString);
    var violation = problem.getViolations().get(0);
    assertEquals(List.of(field, message), List.of(violation.getField(), violation.getMessage()));
    assertEquals(null, violation.getRejectedValue());
  }
}
