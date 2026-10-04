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
import com.forwardmeasure.platform.server.jaxrs.ConstraintViolationProblems;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;

/**
 * Bean-validation failures as the same RFC 9457 problem Quarkus and Spring return
 * (forwardmeasure-platform-server-jaxrs's {@code ConstraintViolationExceptionMapper}). Micronaut
 * validates outside JAX-RS and routes the failure to an {@link ExceptionHandler}, which outranks
 * the JAX-RS bridge's mappers, so this replaces micronaut-validation's own handler instead of
 * registering the JAX-RS mapper.
 */
@Singleton
@Produces("application/problem+json")
@Replaces(ConstraintExceptionHandler.class)
public class MicronautConstraintViolationProblemHandler
    implements ExceptionHandler<ConstraintViolationException, HttpResponse<Problem>> {

  @Override
  @SuppressWarnings("rawtypes") // ExceptionHandler.handle declares a raw HttpRequest
  public HttpResponse<Problem> handle(HttpRequest request, ConstraintViolationException exception) {
    Problem problem = ConstraintViolationProblems.problem(exception.getConstraintViolations());
    return HttpResponse.<Problem>status(HttpStatus.valueOf(problem.getStatus()))
        .contentType("application/problem+json")
        .body(problem);
  }
}
