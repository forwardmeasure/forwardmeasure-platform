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
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;

/** The Micronaut side of {@link RequestProblems}: responses, and arguments by their HTTP name. */
final class MicronautRequestProblems {
  private static final String[] JAXRS_PARAMS = {
    "jakarta.ws.rs.HeaderParam",
    "jakarta.ws.rs.QueryParam",
    "jakarta.ws.rs.PathParam",
    "jakarta.ws.rs.CookieParam",
    "jakarta.ws.rs.FormParam",
    "jakarta.ws.rs.MatrixParam"
  };

  private MicronautRequestProblems() {}

  static HttpResponse<Problem> response(Problem problem) {
    return HttpResponse.<Problem>status(HttpStatus.valueOf(problem.getStatus()))
        .contentType("application/problem+json")
        .body(problem);
  }

  /** The request body: explicitly {@code @Body}, or a JAX-RS entity parameter (no binding). */
  static boolean isBody(Argument<?> argument) {
    AnnotationMetadata metadata = argument.getAnnotationMetadata();
    return metadata.hasStereotype(Body.class) || !metadata.hasStereotype(Bindable.class);
  }

  /** A query parameter, by Micronaut's annotation or the JAX-RS one. */
  static boolean isQuery(Argument<?> argument) {
    AnnotationMetadata metadata = argument.getAnnotationMetadata();
    return metadata.hasStereotype(QueryValue.class)
        || metadata.hasAnnotation("jakarta.ws.rs.QueryParam");
  }

  /** A path parameter, by Micronaut's annotation or the JAX-RS one. */
  static boolean isPath(Argument<?> argument) {
    AnnotationMetadata metadata = argument.getAnnotationMetadata();
    return metadata.hasStereotype(PathVariable.class)
        || metadata.hasAnnotation("jakarta.ws.rs.PathParam");
  }

  /** The argument's name in the request - e.g. {@code If-Match} rather than {@code ifMatch}. */
  static String httpName(Argument<?> argument) {
    if (isBody(argument)) return RequestProblems.BODY;
    AnnotationMetadata metadata = argument.getAnnotationMetadata();
    for (String jaxrs : JAXRS_PARAMS) {
      var name = metadata.stringValue(jaxrs);
      if (name.isPresent()) return name.get();
    }
    return metadata.stringValue(Bindable.class).orElse(argument.getName());
  }
}
