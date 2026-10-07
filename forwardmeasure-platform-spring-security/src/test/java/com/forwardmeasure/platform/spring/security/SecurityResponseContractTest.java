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
package com.forwardmeasure.platform.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

class SecurityResponseContractTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void rejectedCredentialsProduceBearerChallengeAndSafeProblemBody() throws Exception {
    var response = new MockHttpServletResponse();
    new ProblemAuthenticationEntryPoint(mapper)
        .commence(
            new MockHttpServletRequest("GET", "/api/entities"),
            response,
            new BadCredentialsException("private token validation detail"));
    assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
    assertProblem(response, 401, "Unauthorized");
    assertFalse(response.getContentAsString().contains("private token"));
  }

  @Test
  void authenticatedAccessDenialProducesForbiddenWithoutCredentialChallenge() throws Exception {
    var response = new MockHttpServletResponse();
    new ProblemAccessDeniedHandler(mapper)
        .handle(
            new MockHttpServletRequest("POST", "/api/entities"),
            response,
            new AccessDeniedException("private policy and role details"));
    assertNull(response.getHeader("WWW-Authenticate"));
    assertProblem(response, 403, "Forbidden");
    assertFalse(response.getContentAsString().contains("private policy"));
  }

  private void assertProblem(MockHttpServletResponse response, int status, String title)
      throws Exception {
    assertEquals(status, response.getStatus());
    assertEquals("application/problem+json", response.getContentType());
    var body = mapper.readTree(response.getContentAsByteArray());
    assertEquals("about:blank", body.path("type").asText());
    assertEquals(status, body.path("status").asInt());
    assertEquals(title, body.path("title").asText());
    assertEquals("HTTP " + status + " " + title, body.path("detail").asText());
  }
}
