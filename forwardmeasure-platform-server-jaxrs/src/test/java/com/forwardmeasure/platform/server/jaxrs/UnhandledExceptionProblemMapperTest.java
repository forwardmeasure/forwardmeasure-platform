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
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardmeasure.openworkflow.common.model.Problem;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class UnhandledExceptionProblemMapperTest {
  private final UnhandledExceptionProblemMapper mapper = new UnhandledExceptionProblemMapper();

  @Test
  void anUnexpectedFailureIsAGeneric500ThatExposesNothing() {
    try (Response response =
        mapper.toResponse(new IllegalStateException("database password=secret"))) {
      Problem problem = (Problem) response.getEntity();

      assertEquals(500, response.getStatus());
      assertEquals("application/problem+json", response.getMediaType().toString());
      assertEquals("Internal Server Error", problem.getTitle());
      assertEquals("Unexpected API failure", problem.getDetail());
    }
  }

  @Test
  void aRuntimeHttpErrorKeepsItsStatusWithTheJaxRsDetail() {
    try (Response response = mapper.toResponse(new NotFoundException())) {
      Problem problem = (Problem) response.getEntity();

      assertEquals(404, response.getStatus());
      assertEquals("Not Found", problem.getTitle());
      assertEquals("HTTP 404 Not Found", problem.getDetail());
    }
    try (Response response = mapper.toResponse(new NotSupportedException())) {
      assertEquals(415, response.getStatus());
      assertEquals("Unsupported Media Type", ((Problem) response.getEntity()).getTitle());
    }
  }

  @Test
  void aResponseThatAlreadyHasABodyIsKept() {
    Response own = Response.status(409).entity("own body").build();

    assertSame(own, mapper.toResponse(new WebApplicationException(own)));
  }

  /** Quarkus wraps a body it can't parse in a bare 400 instead of throwing Jackson's exception. */
  @Test
  void aWrappedJsonBindingFailureIsTheContractProblemForTheBody() {
    JsonProcessingException notJson =
        org.junit.jupiter.api.Assertions.assertThrows(
            JsonProcessingException.class, () -> new ObjectMapper().readTree("{\"a\":"));

    try (Response response =
        mapper.toResponse(new WebApplicationException(notJson, Response.Status.BAD_REQUEST))) {
      Problem problem = (Problem) response.getEntity();

      assertEquals(400, response.getStatus());
      assertEquals(RequestProblems.BODY, problem.getViolations().get(0).getField());
      assertEquals(RequestProblems.NOT_JSON, problem.getViolations().get(0).getMessage());
    }
  }
}
