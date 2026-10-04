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
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ConversionErrorHandler;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import java.util.List;

/**
 * An argument Micronaut couldn't bind, as Quarkus and Spring answer it. A request body is a 400
 * naming the offending field ({@link MicronautBindingProblems}); a query parameter is the 400
 * naming the parameter and the rejected value, as forwardmeasure-platform-server-jaxrs's {@code
 * QueryParameterConversionFilter} answers it there; a path parameter (an id that doesn't convert)
 * is a 404; anything else a 400. Micronaut's own annotations and the JAX-RS ones a bridged resource
 * carries are both recognized.
 */
@Singleton
@Produces("application/problem+json")
@Replaces(ConversionErrorHandler.class)
public class MicronautConversionErrorProblemHandler
    implements ExceptionHandler<ConversionErrorException, HttpResponse<Problem>> {

  @Override
  @SuppressWarnings("rawtypes") // ExceptionHandler.handle declares a raw HttpRequest
  public HttpResponse<Problem> handle(HttpRequest request, ConversionErrorException exception) {
    Argument<?> argument = exception.getArgument();
    if (MicronautRequestProblems.isBody(argument)) {
      return MicronautRequestProblems.response(
          MicronautBindingProblems.problem(exception.getCause()));
    }
    if (MicronautRequestProblems.isQuery(argument)) {
      Object rejected = exception.getConversionError().getOriginalValue().orElse(null);
      return MicronautRequestProblems.response(
          RequestProblems.badRequest(
              List.of(
                  RequestProblems.violation(
                      MicronautRequestProblems.httpName(argument),
                      RequestProblems.INVALID_VALUE,
                      singleValue(rejected)))));
    }
    int status = MicronautRequestProblems.isPath(argument) ? 404 : 400;
    return MicronautRequestProblems.response(RequestProblems.httpError(status));
  }

  /** Micronaut hands over a query parameter's raw value as a list of strings. */
  private static Object singleValue(Object raw) {
    return raw instanceof List<?> values && values.size() == 1 ? values.get(0) : raw;
  }
}
