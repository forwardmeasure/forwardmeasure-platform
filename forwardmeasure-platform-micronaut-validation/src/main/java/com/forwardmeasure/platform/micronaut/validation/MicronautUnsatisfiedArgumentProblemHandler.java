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
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.UnsatisfiedArgumentHandler;
import jakarta.inject.Singleton;
import java.util.List;

/**
 * A required argument the request left out. Quarkus and Spring bind it as null and bean validation
 * reports it; Micronaut refuses to bind it at all, so this reports it the same way.
 */
@Singleton
@Produces("application/problem+json")
@Replaces(UnsatisfiedArgumentHandler.class)
public class MicronautUnsatisfiedArgumentProblemHandler
    implements ExceptionHandler<UnsatisfiedArgumentException, HttpResponse<Problem>> {

  @Override
  @SuppressWarnings("rawtypes") // ExceptionHandler.handle declares a raw HttpRequest
  public HttpResponse<Problem> handle(HttpRequest request, UnsatisfiedArgumentException exception) {
    String field = MicronautRequestProblems.httpName(exception.getArgument());
    return MicronautRequestProblems.response(
        RequestProblems.badRequest(
            List.of(RequestProblems.violation(field, RequestProblems.REQUIRED, null))));
  }
}
