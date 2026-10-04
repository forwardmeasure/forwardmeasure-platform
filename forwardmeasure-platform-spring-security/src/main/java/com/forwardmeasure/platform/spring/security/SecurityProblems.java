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
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** The problem response for a status Spring Security answers with before Jersey runs. */
final class SecurityProblems {
  private SecurityProblems() {}

  static void write(HttpServletResponse response, int status, ObjectMapper mapper)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    mapper.writeValue(response.getOutputStream(), RequestProblems.httpError(status));
  }
}
