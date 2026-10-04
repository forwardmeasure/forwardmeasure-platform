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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.forwardmeasure.openworkflow.common.model.Problem;
import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Last resort for anything no specific mapper covers, the same on every service and framework. A
 * JAX-RS HTTP error (unknown path, wrong method or media type) keeps its own status as a problem; a
 * body the runtime couldn't bind is the same 400 the Jackson mappers give (Quarkus wraps some
 * binding failures in a bare 400 rather than throwing Jackson's own exception); anything else is a
 * 500 that says nothing about the server's internals - the cause is logged here instead.
 */
@Provider
@Priority(Integer.MAX_VALUE)
public class UnhandledExceptionProblemMapper implements ExceptionMapper<Throwable> {
  private static final Logger LOG = LoggerFactory.getLogger(UnhandledExceptionProblemMapper.class);

  @Override
  public Response toResponse(Throwable exception) {
    JsonProcessingException json = jsonCause(exception);
    if (json != null && JsonBindingProblems.isRequestFault(json)) {
      return JsonBindingProblems.response(json);
    }
    if (exception instanceof WebApplicationException web && web.getResponse() != null) {
      Response response = web.getResponse();
      if (response.hasEntity()) return response;
      if (response.getStatus() < 500) {
        Response.StatusType status = response.getStatusInfo();
        return response(
            RequestProblems.httpError(
                status.getStatusCode(), status.getReasonPhrase(), clientErrorDetail(web)));
      }
    }
    LOG.error("Unexpected API failure", exception);
    return response(
        new Problem()
            .type("about:blank")
            .title("Internal Server Error")
            .status(500)
            .detail("Unexpected API failure"));
  }

  /**
   * The detail for a client error the runtime or a resource raised: the exception's message, which
   * JAX-RS defaults to e.g. {@code HTTP 404 Not Found}. Null gives that default.
   */
  protected String clientErrorDetail(WebApplicationException exception) {
    return exception.getMessage();
  }

  private static JsonProcessingException jsonCause(Throwable exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof JsonProcessingException json) return json;
      if (cause.getCause() == cause) break;
    }
    return null;
  }

  private static Response response(Problem problem) {
    return Response.status(problem.getStatus())
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
