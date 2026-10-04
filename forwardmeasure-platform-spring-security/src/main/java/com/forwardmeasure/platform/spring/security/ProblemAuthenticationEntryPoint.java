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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * No valid bearer token - none sent, or one that fails validation: a 401 problem with {@code
 * WWW-Authenticate: Bearer}, exactly as Quarkus and Micronaut answer. Spring's own
 * BearerTokenAuthenticationEntryPoint sends no body and adds RFC 6750 error parameters the other
 * two don't. Pass the application's {@link ObjectMapper} - the one Jersey writes every other
 * problem with.
 */
public final class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper mapper;

  public ProblemAuthenticationEntryPoint(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setHeader("WWW-Authenticate", "Bearer");
    SecurityProblems.write(response, HttpServletResponse.SC_UNAUTHORIZED, mapper);
  }
}
