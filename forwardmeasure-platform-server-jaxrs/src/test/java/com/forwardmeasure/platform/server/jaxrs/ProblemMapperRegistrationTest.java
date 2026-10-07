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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.forwardmeasure.openworkflow.common.model.Problem;
import jakarta.ws.rs.core.FeatureContext;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProblemMapperRegistrationTest {
  @Test
  void registersEveryProviderForRuntimesWithoutAutomaticDiscovery() {
    var registered = new ArrayList<Class<?>>();
    FeatureContext context =
        (FeatureContext)
            Proxy.newProxyInstance(
                FeatureContext.class.getClassLoader(),
                new Class<?>[] {FeatureContext.class},
                (proxy, method, arguments) -> {
                  if (!method.getName().equals("register")) throw new AssertionError(method);
                  registered.add((Class<?>) arguments[0]);
                  return proxy;
                });
    assertTrue(new RequestProblemsFeature().configure(context));
    assertEquals(
        java.util.Set.of(
            ConstraintViolationExceptionMapper.class,
            JsonProcessingProblemMapper.class,
            JsonParseProblemMapper.class,
            JsonMappingProblemMapper.class,
            MismatchedInputProblemMapper.class,
            UnhandledExceptionProblemMapper.class,
            QueryParameterConversionFilter.class),
        java.util.Set.copyOf(registered));
    assertEquals(7, registered.size(), "providers must not be registered twice");
  }

  @Test
  void specificJacksonProvidersReturnTheSameClientProblemContract() {
    var mapper = new ObjectMapper();
    var malformed = assertThrows(JsonParseException.class, () -> mapper.readTree("{"));
    var mismatched =
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("[]", Integer.class));
    var responses =
        List.of(
            new JsonParseProblemMapper().toResponse(malformed),
            new JsonProcessingProblemMapper().toResponse(malformed),
            new JsonMappingProblemMapper().toResponse(mismatched),
            new MismatchedInputProblemMapper().toResponse(mismatched));
    for (var response : responses) {
      try (response) {
        assertEquals(400, response.getStatus());
        assertEquals("application/problem+json", response.getMediaType().toString());
        assertEquals(
            "body", ((Problem) response.getEntity()).getViolations().getFirst().getField());
      }
    }
  }

  @Test
  void serverSideMappingFaultsRemainServerErrorsWithoutLeakingDetails() {
    var failure =
        JsonMappingException.fromUnexpectedIOE(new java.io.IOException("private database detail"));
    try (var response = new JsonMappingProblemMapper().toResponse(failure)) {
      assertEquals(500, response.getStatus());
      assertEquals("Internal Server Error", ((Problem) response.getEntity()).getTitle());
      org.junit.jupiter.api.Assertions.assertFalse(
          ((Problem) response.getEntity()).getDetail().contains("private database detail"));
    }
  }
}
