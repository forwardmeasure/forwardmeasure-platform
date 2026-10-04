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

import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.authentication.DefaultAuthorizationExceptionHandler;
import jakarta.inject.Singleton;

/**
 * A request Micronaut security turned away - no valid bearer token (401) or a caller the rules
 * don't allow (403) - as the RFC 9457 problem Quarkus and Spring return, instead of security's own
 * bare status. A 401 carries the {@code WWW-Authenticate: Bearer} challenge the other two send;
 * security's own handler sends none for a bearer-only service. Active only where micronaut-security
 * is on the classpath - it is optional for this module.
 */
@Singleton
@Produces("application/problem+json")
@Requires(classes = AuthorizationException.class)
@Replaces(DefaultAuthorizationExceptionHandler.class)
public class MicronautAuthorizationProblemHandler
    implements ExceptionHandler<AuthorizationException, HttpResponse<Problem>> {

  @Override
  @SuppressWarnings("rawtypes") // ExceptionHandler.handle declares a raw HttpRequest
  public HttpResponse<Problem> handle(HttpRequest request, AuthorizationException exception) {
    if (exception.isForbidden()) {
      return MicronautRequestProblems.response(RequestProblems.httpError(403));
    }
    return HttpResponse.<Problem>status(HttpStatus.UNAUTHORIZED)
        .contentType("application/problem+json")
        .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .body(RequestProblems.httpError(401));
  }
}
