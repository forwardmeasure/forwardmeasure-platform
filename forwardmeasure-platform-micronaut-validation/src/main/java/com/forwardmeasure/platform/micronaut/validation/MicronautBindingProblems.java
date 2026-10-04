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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.forwardmeasure.openworkflow.common.model.Problem;
import com.forwardmeasure.platform.server.jaxrs.JsonBindingProblems;
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import io.micronaut.json.JsonSyntaxException;
import java.util.List;

/**
 * A request body that didn't bind, as the problem Quarkus and Spring give it. Bodies are read by
 * {@link JacksonRequestBodyReader}, so the cause is Jackson's own exception and maps exactly as it
 * does there ({@link JsonBindingProblems}); a body some other Micronaut reader handled reports only
 * whether it was JSON at all.
 */
final class MicronautBindingProblems {
  private MicronautBindingProblems() {}

  static Problem problem(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = next(cause)) {
      if (cause instanceof JsonProcessingException jackson) {
        return JsonBindingProblems.problem(jackson);
      }
      if (cause instanceof JsonSyntaxException
          || cause instanceof tools.jackson.core.exc.StreamReadException) {
        return body(RequestProblems.NOT_JSON);
      }
    }
    return body(RequestProblems.INVALID_VALUE);
  }

  static Problem body(String message) {
    return RequestProblems.badRequest(
        List.of(RequestProblems.violation(RequestProblems.BODY, message, null)));
  }

  private static Throwable next(Throwable cause) {
    return cause.getCause() == cause ? null : cause.getCause();
  }
}
