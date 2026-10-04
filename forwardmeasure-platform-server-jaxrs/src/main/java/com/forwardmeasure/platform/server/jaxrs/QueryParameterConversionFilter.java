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

import com.forwardmeasure.openworkflow.common.model.Violation;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Refuses a query parameter the runtime couldn't convert to its declared type - an enum value it
 * doesn't know, a number or UUID that isn't one, a date-time that isn't RFC 3339 - as the 400
 * problem naming the parameter and the rejected value, before the runtime tries. Left to the
 * runtime it's a 404 (JAX-RS's rule for a query parameter it can't convert, which Quarkus and
 * Jersey follow), while Micronaut answered 400 naming nothing; the platform's API contracts answer
 * a request they can't use with 400 (list and search operations declare no 404). Path parameters
 * are left alone: an id that doesn't convert is still a 404. {@code
 * MicronautConversionErrorProblemHandler} gives Micronaut the same answers.
 *
 * <p>Looks the parameters up on every declaration of the matched method, as {@link
 * ConstraintViolationProblems} does: a generated interface carries the {@code @QueryParam}s, its
 * implementation doesn't.
 */
@Provider
@Priority(Priorities.USER)
public class QueryParameterConversionFilter implements ContainerRequestFilter {
  @Context ResourceInfo resource;

  @Override
  public void filter(ContainerRequestContext request) {
    Method method = resource == null ? null : resource.getResourceMethod();
    if (method == null) {
      return;
    }
    List<Violation> violations = new ArrayList<>();
    var query = request.getUriInfo().getQueryParameters();
    Type[] types = method.getGenericParameterTypes();
    for (int index = 0; index < types.length; index++) {
      String name = queryParameterName(resource.getResourceClass(), method, index);
      if (name == null || !query.containsKey(name)) {
        continue;
      }
      Class<?> element = elementType(types[index]);
      boolean list = element != null;
      for (String value : query.get(name)) {
        // A list parameter is form-style, explode: false - comma-separated - in the contract.
        for (String item : list ? value.split(",", -1) : new String[] {value}) {
          if (!converts(list ? element : raw(types[index]), item)) {
            violations.add(RequestProblems.violation(name, RequestProblems.INVALID_VALUE, value));
            break;
          }
        }
      }
    }
    if (!violations.isEmpty()) {
      request.abortWith(
          Response.status(Response.Status.BAD_REQUEST)
              .type("application/problem+json")
              .entity(RequestProblems.badRequest(violations))
              .build());
    }
  }

  /** The element class of a {@code List<E>} parameter; null for any other parameter. */
  private static Class<?> elementType(Type type) {
    if (type instanceof ParameterizedType parameterized
        && parameterized.getRawType() == List.class
        && parameterized.getActualTypeArguments()[0] instanceof Class<?> element) {
      return element;
    }
    return null;
  }

  private static Class<?> raw(Type type) {
    return type instanceof ParameterizedType parameterized
        ? (Class<?>) parameterized.getRawType()
        : (Class<?>) type;
  }

  /** Whether {@code value} converts to {@code type} the way the runtimes convert it. */
  static boolean converts(Class<?> type, String value) {
    try {
      if (type == Integer.class || type == int.class) {
        Integer.valueOf(value);
      } else if (type == Long.class || type == long.class) {
        Long.valueOf(value);
      } else if (type == UUID.class) {
        UUID.fromString(value);
      } else if (type == Date.class
          || type == OffsetDateTime.class
          || type == java.time.Instant.class) {
        // A date-time parameter is any RFC 3339 timestamp: an offset, any fraction of a second.
        OffsetDateTime.parse(value);
      } else if (type.isEnum()) {
        return enumConstant(type, value);
      }
      return true;
    } catch (IllegalArgumentException | DateTimeParseException invalid) {
      return false;
    }
  }

  /**
   * The generated enums convert through {@code fromValue} (their wire values are lowercase), and
   * throw {@code IllegalArgumentException} for anything else; a plain enum through {@code valueOf}.
   */
  private static boolean enumConstant(Class<?> type, String value) {
    try {
      Method fromValue = type.getMethod("fromValue", String.class);
      return fromValue.invoke(null, value) != null;
    } catch (NoSuchMethodException plain) {
      for (Object constant : type.getEnumConstants()) {
        if (((Enum<?>) constant).name().equals(value)) {
          return true;
        }
      }
      return false;
    } catch (ReflectiveOperationException rejected) {
      return false;
    }
  }

  /** The {@code @QueryParam} name of the method's {@code index}th parameter, on any declaration. */
  private static String queryParameterName(Class<?> resourceClass, Method method, int index) {
    for (Class<?> type = resourceClass; type != null && type != Object.class; ) {
      String name = declaredName(type, method, index);
      if (name != null) {
        return name;
      }
      for (Class<?> contract : type.getInterfaces()) {
        name = declaredName(contract, method, index);
        if (name != null) {
          return name;
        }
      }
      type = type.getSuperclass();
    }
    return null;
  }

  private static String declaredName(Class<?> type, Method method, int index) {
    try {
      Method declared = type.getDeclaredMethod(method.getName(), method.getParameterTypes());
      for (Annotation annotation : declared.getParameterAnnotations()[index]) {
        if (annotation instanceof QueryParam query) {
          return query.value();
        }
      }
    } catch (NoSuchMethodException absent) {
      // not declared at this level
    }
    return null;
  }
}
