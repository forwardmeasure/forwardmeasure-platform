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

import com.forwardmeasure.openworkflow.common.model.Problem;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Bean-validation failures as {@link ConstraintViolationProblems} problems. Typed on {@link
 * ConstraintViolationException}, so JAX-RS picks it over the runtime's own {@code
 * ExceptionMapper<ValidationException>} (Quarkus's and Jersey's): the most specific mapper wins.
 * Quarkus discovers it through this jar's Jandex index; Spring's Jersey bindings register it with
 * their other mappers. Micronaut handles the exception outside JAX-RS - see the Micronaut binding's
 * {@code MicronautConstraintViolationProblemHandler}.
 */
@Provider
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    Problem problem = ConstraintViolationProblems.problem(exception.getConstraintViolations());
    return Response.status(problem.getStatus())
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
