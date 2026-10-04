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
package com.forwardmeasure.platform.quarkus.security;

import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/** The problem responses for what Quarkus security raises inside JAX-RS. */
final class SecurityProblems {
  private SecurityProblems() {}

  /** No valid bearer token, with the challenge Spring and Micronaut send. */
  static Response unauthorized() {
    return response(RequestProblems.httpError(401))
        .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .build();
  }

  static Response forbidden() {
    return response(RequestProblems.httpError(403)).build();
  }

  private static Response.ResponseBuilder response(Problem problem) {
    return Response.status(problem.getStatus()).type("application/problem+json").entity(problem);
  }
}
