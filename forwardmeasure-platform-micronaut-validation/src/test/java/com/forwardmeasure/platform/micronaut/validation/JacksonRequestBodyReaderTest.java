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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import io.micronaut.core.type.Argument;
import io.micronaut.http.MediaType;
import io.micronaut.http.codec.CodecException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The reader binds a body exactly as Quarkus's and Spring's Jackson do, and a failure maps to the
 * same problem through Micronaut's conversion-error handling.
 */
class JacksonRequestBodyReaderTest {
  private final JacksonRequestBodyReader<Object> reader = new JacksonRequestBodyReader<>();

  static final class Request {
    private final Long count;
    private final Nested nested;
    private OffsetDateTime at;
    private Optional<String> note = Optional.empty();

    @JsonCreator
    Request(
        @JsonProperty(required = true, value = "count") Long count,
        @JsonProperty(required = true, value = "nested_value") Nested nested) {
      this.count = count;
      this.nested = nested;
    }

    @JsonProperty("at")
    void setAt(OffsetDateTime at) {
      this.at = at;
    }

    @JsonProperty("note")
    void setNote(Optional<String> note) {
      this.note = note;
    }
  }

  static final class Nested {
    final String value;

    @JsonCreator
    Nested(@JsonProperty(required = true, value = "value") String value) {
      this.value = value;
    }
  }

  @Test
  void aValidBodyBindsIncludingJavaTimeAndOptional() {
    Request request =
        read(
            "{\"count\":2,\"nested_value\":{\"value\":\"v\"},\"at\":\"2026-09-30T10:00:00Z\","
                + "\"note\":\"n\",\"unknown\":true}");

    assertEquals(2L, request.count);
    assertEquals("v", request.nested.value);
    assertEquals(OffsetDateTime.parse("2026-09-30T10:00:00Z").toInstant(), request.at.toInstant());
    assertEquals(Optional.of("n"), request.note);
  }

  @Test
  void anEmptyBodyIsAbsent() {
    assertNull(read(""));
  }

  @Test
  void aBooleanIsNotANumberAsOnQuarkusAndSpring() {
    assertProblem(
        "{\"count\":true,\"nested_value\":{\"value\":\"v\"}}", "count", RequestProblems.WRONG_TYPE);
  }

  @Test
  void aMissingNestedFieldIsNamedByItsJsonPath() {
    assertProblem(
        "{\"count\":1,\"nested_value\":{}}", "nested_value.value", RequestProblems.REQUIRED);
  }

  @Test
  void aBodyThatIsNotJsonIsReportedAgainstTheBody() {
    assertProblem("{\"count\":", RequestProblems.BODY, RequestProblems.NOT_JSON);
  }

  @Test
  void onlyObjectBodiesAreReadHere() {
    assertTrue(reader.isReadable(argument(Request.class), MediaType.APPLICATION_JSON_TYPE));
    assertFalse(reader.isReadable(argument(String.class), MediaType.APPLICATION_JSON_TYPE));
    assertFalse(reader.isReadable(argument(byte[].class), MediaType.APPLICATION_JSON_TYPE));
  }

  @Test
  void leavesStreamingReactiveAndFrameworkOwnedBodiesToTheirDedicatedReaders() {
    for (Class<?> type :
        List.of(
            java.io.InputStream.class,
            org.reactivestreams.Publisher.class,
            java.util.concurrent.CompletionStage.class,
            io.micronaut.http.HttpRequest.class,
            java.nio.ByteBuffer.class)) {
      assertFalse(
          reader.isReadable(argument(type), MediaType.APPLICATION_JSON_TYPE), type.getName());
    }
  }

  private Request read(String body) {
    return (Request)
        reader.read(
            argument(Request.class),
            MediaType.APPLICATION_JSON_TYPE,
            null,
            new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
  }

  private void assertProblem(String body, String field, String message) {
    CodecException failure = assertThrows(CodecException.class, () -> read(body));

    Problem problem = MicronautBindingProblems.problem(failure);

    assertEquals(400, problem.getStatus());
    assertEquals(1, problem.getViolations().size(), problem::toString);
    assertEquals(
        List.of(field, message),
        List.of(
            problem.getViolations().get(0).getField(),
            problem.getViolations().get(0).getMessage()));
  }

  @SuppressWarnings("unchecked")
  private static Argument<Object> argument(Class<?> type) {
    return (Argument<Object>) Argument.of(type);
  }
}
