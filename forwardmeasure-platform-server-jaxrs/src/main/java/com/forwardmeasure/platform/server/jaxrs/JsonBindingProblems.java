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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.forwardmeasure.openworkflow.common.model.Problem;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A request body Jackson couldn't bind to the contract's model, as the same problem bean validation
 * produces ({@link RequestProblems}). Jackson's path names each field as the client wrote it, the
 * missing property included when a required one is absent. Every framework binds bodies with
 * Jackson (Micronaut included - see forwardmeasure-platform-micronaut-validation's {@code
 * JacksonRequestBodyReader}), so this is the one mapping for all of them. No rejected value: the
 * field names the input, and what Jackson can recover of it varies with the failure.
 *
 * <p>Jackson raises the same exception types for the server's own faults - a model it can't bind at
 * all, a response it can't write - and those stay a 500.
 */
public final class JsonBindingProblems {
  private static final Logger LOG = LoggerFactory.getLogger(JsonBindingProblems.class);

  // Jackson reports a required (creator) property that's absent or null only through its message.
  private static final List<String> MISSING_REQUIRED =
      List.of("Missing required creator property", "Null value for creator property");
  // An empty body - absent, as Quarkus's and Jersey's readers treat it before this is reached.
  private static final String NO_CONTENT = "No content to map due to end-of-input";
  private static final Pattern ARGUMENT_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

  private JsonBindingProblems() {}

  /** The response every framework's Jackson binding-failure mapper returns. */
  public static Response response(JsonProcessingException exception) {
    Problem problem = problem(exception);
    if (problem.getStatus() >= 500) {
      LOG.error("JSON processing failed on the server side", exception);
    }
    return Response.status(problem.getStatus())
        .type("application/problem+json")
        .entity(problem)
        .build();
  }

  public static Problem problem(JsonProcessingException exception) {
    if (!isRequestFault(exception)) {
      return new Problem()
          .type("about:blank")
          .title("Internal Server Error")
          .status(500)
          .detail("Unexpected failure processing JSON");
    }
    if (!(exception instanceof JsonMappingException mapping)) {
      return single(RequestProblems.BODY, RequestProblems.NOT_JSON);
    }
    String field = field(mapping.getPath());
    if (mapping instanceof UnrecognizedPropertyException) {
      return single(field, RequestProblems.UNKNOWN_FIELD);
    }
    if (mapping instanceof InvalidFormatException) {
      return single(field, RequestProblems.INVALID_VALUE);
    }
    if (mapping instanceof MismatchedInputException) {
      String message = mapping.getOriginalMessage() == null ? "" : mapping.getOriginalMessage();
      boolean missing =
          message.startsWith(NO_CONTENT) || MISSING_REQUIRED.stream().anyMatch(message::startsWith);
      return single(field, missing ? RequestProblems.REQUIRED : RequestProblems.WRONG_TYPE);
    }
    if (mapping instanceof ValueInstantiationException) {
      String argument = refusedNullArgument(mapping);
      if (argument != null) {
        String required = RequestProblems.BODY.equals(field) ? argument : field + "." + argument;
        return single(required, RequestProblems.REQUIRED);
      }
    }
    return single(field, RequestProblems.INVALID_VALUE);
  }

  /**
   * The argument a record or constructor refused as null, by name - {@code
   * Objects.requireNonNull(spec, "spec")} - which is how a model that isn't generated from the
   * contract (e.g. forwardmeasure-data-streaming's request records) says a required field is
   * missing. Null for any other construction failure.
   */
  private static String refusedNullArgument(JsonMappingException exception) {
    if (exception.getCause() instanceof NullPointerException refused
        && refused.getMessage() != null
        && ARGUMENT_NAME.matcher(refused.getMessage()).matches()) {
      return refused.getMessage();
    }
    return null;
  }

  /**
   * Whether the JSON the client sent is at fault: raised while reading, never a model Jackson can't
   * bind ({@link InvalidDefinitionException}) or a failure writing a response.
   */
  static boolean isRequestFault(JsonProcessingException exception) {
    if (exception instanceof InvalidDefinitionException) return false;
    if (exception instanceof StreamReadException) return true;
    return exception instanceof JsonMappingException mapping
        && mapping.getProcessor() instanceof JsonParser;
  }

  private static Problem single(String field, String message) {
    return RequestProblems.badRequest(List.of(RequestProblems.violation(field, message, null)));
  }

  /** e.g. {@code steps[2].display_name}; {@code body} when Jackson gives no path. */
  static String field(List<JsonMappingException.Reference> path) {
    StringBuilder field = new StringBuilder();
    for (JsonMappingException.Reference reference : path) {
      if (reference.getFieldName() != null) {
        if (!field.isEmpty()) field.append('.');
        field.append(reference.getFieldName());
      } else if (reference.getIndex() >= 0) {
        field.append('[').append(reference.getIndex()).append(']');
      }
    }
    return field.isEmpty() ? RequestProblems.BODY : field.toString();
  }
}
