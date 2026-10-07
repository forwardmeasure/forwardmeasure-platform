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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MicronautArgumentProblemTest {
  // Real compile-time metadata: the same annotation mappings used by Micronaut's binders.
  @Introspected
  record Inputs(
      @Body String body,
      String implicitBody,
      @QueryValue("page_size") Integer pageSize,
      @PathVariable("entityId") UUID id,
      @Header("If-Match") String etag,
      @Header String trace) {}

  @Introspected
  @jakarta.ws.rs.Path("/binding-contract")
  public static class JaxRsInputs {
    @io.micronaut.context.annotation.Executable
    @jakarta.ws.rs.GET
    @jakarta.ws.rs.Path("/{taskId}")
    public String route(
        @jakarta.ws.rs.QueryParam("ids") List<UUID> ids,
        @jakarta.ws.rs.PathParam("taskId") UUID taskId) {
      return "";
    }
  }

  @Test
  void missingInputsNameTheirHttpBindingRatherThanTheirJavaParameter() {
    var handler = new MicronautUnsatisfiedArgumentProblemHandler();
    for (var entry :
        java.util.Map.of(
                "body",
                "body",
                "implicitBody",
                "body",
                "pageSize",
                "page_size",
                "etag",
                "If-Match",
                "trace",
                "trace",
                "ids",
                "ids",
                "taskId",
                "taskId")
            .entrySet()) {
      var response =
          handler.handle(
              HttpRequest.GET("/"), new UnsatisfiedArgumentException(argument(entry.getKey())));
      assertViolation(response, entry.getValue(), RequestProblems.REQUIRED, null);
    }
  }

  @Test
  void malformedQueriesAreBadRequestsButMalformedPathIdsAreNotFound() {
    var handler = new MicronautConversionErrorProblemHandler();
    assertViolation(
        handler.handle(HttpRequest.GET("/?page_size=bad"), failure("pageSize", List.of("bad"))),
        "page_size",
        RequestProblems.INVALID_VALUE,
        "bad");
    assertViolation(
        handler.handle(HttpRequest.GET("/?ids=bad"), failure("ids", List.of("bad"))),
        "ids",
        RequestProblems.INVALID_VALUE,
        "bad");
    for (String name : List.of("id", "taskId")) {
      var response = handler.handle(HttpRequest.GET("/bad"), failure(name, "bad"));
      assertEquals(404, response.code());
      assertEquals("Not Found", response.body().getTitle());
    }
    assertEquals(400, handler.handle(HttpRequest.GET("/"), failure("etag", "bad")).code());
  }

  @Test
  void echoesOnlyASingleScalarQueryValue() {
    var handler = new MicronautConversionErrorProblemHandler();
    assertViolation(
        handler.handle(HttpRequest.GET("/"), failure("pageSize", "bad")),
        "page_size",
        RequestProblems.INVALID_VALUE,
        "bad");
    assertViolation(
        handler.handle(HttpRequest.GET("/"), failure("pageSize", List.of("bad", "other"))),
        "page_size",
        RequestProblems.INVALID_VALUE,
        null);
    assertViolation(
        handler.handle(HttpRequest.GET("/"), failure("pageSize", null)),
        "page_size",
        RequestProblems.INVALID_VALUE,
        null);
  }

  @Test
  void wrappedJacksonBodyFailuresRetainTheirFieldAndDoNotEchoTheBody() {
    var jackson = new com.fasterxml.jackson.databind.ObjectMapper();
    var failure =
        assertThrows(
            com.fasterxml.jackson.databind.exc.MismatchedInputException.class,
            () -> jackson.readValue("{\"count\":true}", CountBody.class));
    var conversion =
        new ConversionErrorException(
            argument("body"), new IllegalArgumentException("binder wrapper", failure));
    assertViolation(
        new MicronautConversionErrorProblemHandler()
            .handle(HttpRequest.POST("/", "private input"), conversion),
        "count",
        RequestProblems.WRONG_TYPE,
        null);
  }

  public static class CountBody {
    public Integer count;
  }

  private static Argument<?> argument(String name) {
    Argument<?>[] arguments =
        name.equals("ids") || name.equals("taskId")
            ? BeanIntrospection.getIntrospection(JaxRsInputs.class).getBeanMethods().stream()
                .filter(method -> method.getName().equals("route"))
                .findFirst()
                .orElseThrow()
                .getArguments()
            : BeanIntrospection.getIntrospection(Inputs.class).getConstructorArguments();
    return Arrays.stream(arguments)
        .filter(argument -> argument.getName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static ConversionErrorException failure(String name, Object original) {
    return new ConversionErrorException(
        argument(name),
        new ConversionError() {
          public Exception getCause() {
            return new IllegalArgumentException("invalid input");
          }

          public Optional<Object> getOriginalValue() {
            return Optional.ofNullable(original);
          }
        });
  }

  static void assertViolation(
      HttpResponse<Problem> response, String field, String message, String rejected) {
    assertEquals(400, response.code());
    assertEquals("application/problem+json", response.getContentType().orElseThrow().toString());
    var problem = response.body();
    assertEquals(400, problem.getStatus());
    assertEquals(1, problem.getViolations().size());
    var violation = problem.getViolations().getFirst();
    assertEquals(field, violation.getField());
    assertEquals(message, violation.getMessage());
    if (rejected == null) assertNull(violation.getRejectedValue());
    else assertEquals(rejected, violation.getRejectedValue());
  }
}
