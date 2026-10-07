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
package com.forwardmeasure.platform.micronaut.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.json.JsonSyntaxException;
import io.micronaut.validation.validator.Validator;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

class MicronautExceptionContractTest {
  @Introspected
  public static class ValidatedBody {
    @NotNull
    @com.fasterxml.jackson.annotation.JsonProperty("display_name")
    public String displayName;

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String value) {
      displayName = value;
    }
  }

  @Test
  void beanValidationUsesTheSameWireNamesAsJackson() {
    var violations = Validator.getInstance().validate(new ValidatedBody());
    assertEquals(1, violations.size());
    var response =
        new MicronautConstraintViolationProblemHandler()
            .handle(HttpRequest.POST("/", "{}"), new ConstraintViolationException(violations));
    MicronautArgumentProblemTest.assertViolation(
        response, "display_name", RequestProblems.REQUIRED, null);
  }

  @Test
  void micronautAndJacksonThreeSyntaxFailuresHaveTheSameSafeProblem() {
    var request = HttpRequest.POST("/", "malformed private body");
    var syntax = new JsonSyntaxException("private parser detail");
    MicronautArgumentProblemTest.assertViolation(
        new MicronautJsonSyntaxProblemHandler().handle(request, syntax),
        "body",
        RequestProblems.NOT_JSON,
        null);
    assertEquals(
        RequestProblems.NOT_JSON,
        MicronautBindingProblems.problem(new IllegalStateException(syntax))
            .getViolations()
            .getFirst()
            .getMessage());
    var jackson =
        assertThrows(
            tools.jackson.core.exc.StreamReadException.class,
            () -> {
              try (var parser = new tools.jackson.core.json.JsonFactory().createParser("{broken")) {
                while (parser.nextToken() != null) {
                  /* consume the actual malformed stream */
                }
              }
            });
    MicronautArgumentProblemTest.assertViolation(
        new MicronautJacksonProblemHandler().handle(request, jackson),
        "body",
        RequestProblems.NOT_JSON,
        null);
  }

  @Test
  void unknownBindingFailuresDoNotLeakExceptionMessagesOrLoopOnSelfCauses() {
    var failure = new IllegalArgumentException("private database detail");
    assertEquals(
        RequestProblems.INVALID_VALUE,
        MicronautBindingProblems.problem(failure).getViolations().getFirst().getMessage());
    Throwable selfCausing =
        new RuntimeException("private detail") {
          public synchronized Throwable getCause() {
            return this;
          }
        };
    assertEquals(
        RequestProblems.INVALID_VALUE,
        MicronautBindingProblems.problem(selfCausing).getViolations().getFirst().getMessage());
  }

  @Test
  void routingFailuresNormalizeFrameworkMessagesButPreserveExplicitClientDetails() {
    var mapper = new MicronautUnhandledExceptionProblemMapper();
    var route =
        new WebApplicationException(
            new HttpStatusException(HttpStatus.NOT_FOUND, "framework route detail"), 404);
    try (var response = mapper.toResponse(route)) {
      assertEquals(404, response.getStatus());
      assertEquals("HTTP 404 Not Found", ((Problem) response.getEntity()).getDetail());
    }
    try (var response =
        mapper.toResponse(new WebApplicationException("public client detail", 400))) {
      assertEquals("public client detail", ((Problem) response.getEntity()).getDetail());
    }
    try (var response = mapper.toResponse(new IllegalStateException("private database detail"))) {
      assertEquals(500, response.getStatus());
      assertFalse(((Problem) response.getEntity()).getDetail().contains("private"));
    }
  }
}
