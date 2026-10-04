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
import com.forwardmeasure.openworkflow.common.model.Violation;
import jakarta.ws.rs.core.Response;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The one RFC 9457 problem for a request that breaks the API contract, whichever check caught it -
 * JSON binding or bean validation - and on whichever framework: a 400 whose {@code violations} name
 * each offending input as the client wrote it, with the same message for the same fault.
 */
public final class RequestProblems {
  /** A required value is absent - bean validation's own {@code @NotNull} message. */
  public static final String REQUIRED = "must not be null";

  public static final String INVALID_VALUE = "has an invalid value";
  public static final String WRONG_TYPE = "has the wrong type";
  public static final String UNKNOWN_FIELD = "is not a recognized field";
  public static final String NOT_JSON = "is not valid JSON";

  /** The field name for the request body as a whole. */
  public static final String BODY = "body";

  private static final int MAX_REJECTED_VALUE_LENGTH = 256;

  private RequestProblems() {}

  public static Problem badRequest(List<Violation> violations) {
    return new Problem()
        .type("about:blank")
        .title("Bad Request")
        .status(400)
        .detail("The request does not satisfy the API contract.")
        .violations(
            violations.stream()
                .sorted(
                    Comparator.comparing(Violation::getField).thenComparing(Violation::getMessage))
                .toList());
  }

  /**
   * An HTTP error the runtime raised before the request reached a resource (no such path, a
   * parameter it couldn't convert), with the detail JAX-RS gives it: {@code HTTP 404 Not Found}.
   */
  public static Problem httpError(int status) {
    Response.Status known = Response.Status.fromStatusCode(status);
    return httpError(status, known == null ? "" : known.getReasonPhrase(), null);
  }

  /** As {@link #httpError(int)}, with the runtime's reason phrase and detail when it has them. */
  public static Problem httpError(int status, String reason, String detail) {
    String title = reason == null || reason.isBlank() ? "HTTP " + status : reason;
    return new Problem()
        .type("about:blank")
        .title(title)
        .status(status)
        .detail(detail != null ? detail : ("HTTP " + status + " " + nullToEmpty(reason)).trim());
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  public static Violation violation(String field, String message, Object rejected) {
    return new Violation().field(field).message(message).rejectedValue(rejectedValue(rejected));
  }

  /** Scalar values only - never an object's toString, which could echo a whole request body. */
  static String rejectedValue(Object value) {
    if (!(value instanceof CharSequence
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Enum<?>
        || value instanceof UUID
        || value instanceof TemporalAccessor)) {
      return null;
    }
    String text = String.valueOf(value);
    return text.length() <= MAX_REJECTED_VALUE_LENGTH
        ? text
        : text.substring(0, MAX_REJECTED_VALUE_LENGTH);
  }
}
