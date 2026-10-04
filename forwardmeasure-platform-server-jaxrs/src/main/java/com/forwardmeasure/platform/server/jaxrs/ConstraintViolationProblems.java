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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * The RFC 9457 problem for a bean-validation failure, identical on every framework binding: a
 * request that breaks the generated contract's constraints is a 400 whose {@code violations} name
 * each offending input the way a client wrote it - a header, path or query parameter by its HTTP
 * name, a body field by its path within the body. A response that breaks them is the server's own
 * fault: a 500 without detail.
 */
public final class ConstraintViolationProblems {
  private ConstraintViolationProblems() {}

  public static Problem problem(Set<? extends ConstraintViolation<?>> violations) {
    if (violations.stream().anyMatch(ConstraintViolationProblems::isReturnValue)) {
      return new Problem().type("about:blank").title("Internal Server Error").status(500);
    }
    return RequestProblems.badRequest(
        violations.stream()
            .map(
                violation ->
                    RequestProblems.violation(
                        field(violation), violation.getMessage(), violation.getInvalidValue()))
            .toList());
  }

  private static boolean isReturnValue(ConstraintViolation<?> violation) {
    for (Path.Node node : violation.getPropertyPath()) {
      if (node.getKind() == ElementKind.RETURN_VALUE) return true;
    }
    return false;
  }

  /**
   * The offending input as the client named it: a header/path/query parameter by its HTTP name, a
   * body field by its JSON path, e.g. {@code If-Match} or {@code steps[2].display_name}.
   */
  static String field(ConstraintViolation<?> violation) {
    Deque<String> parts = new ArrayDeque<>();
    List<Method> declarations = List.of();
    // The declared type of the value the next node descends into - resolves JSON names.
    Type current = violation.getRootBeanClass();
    for (Path.Node node : violation.getPropertyPath()) {
      switch (node.getKind()) {
        case METHOD -> declarations = declarations(violation.getRootBeanClass(), node);
        case PARAMETER -> {
          String name = httpName(declarations, node);
          if (name != null) parts.add(name);
          current = parameterType(declarations, node);
        }
        case PROPERTY, CONTAINER_ELEMENT -> {
          // An element's index belongs to the iterable before it: steps[2].name, tags[3].
          if (node.isInIterable()) {
            parts.add(parts.isEmpty() ? index(node) : parts.removeLast() + index(node));
            current = JsonFieldNames.element(current);
          }
          String name = node.getName();
          if (name != null && !name.startsWith("<")) {
            Class<?> owner = JsonFieldNames.raw(current);
            parts.add(JsonFieldNames.of(owner, name));
            current = JsonFieldNames.typeOf(owner, name);
          }
        }
        default -> {}
      }
    }
    return parts.isEmpty() ? RequestProblems.BODY : String.join(".", parts);
  }

  private static Type parameterType(List<Method> declarations, Path.Node node) {
    if (declarations.isEmpty()) return null;
    try {
      int index = node.as(Path.ParameterNode.class).getParameterIndex();
      Type[] types = declarations.get(0).getGenericParameterTypes();
      return index >= 0 && index < types.length ? types[index] : null;
    } catch (ClassCastException unsupported) {
      return null;
    }
  }

  private static String index(Path.Node node) {
    if (node.getIndex() != null) return "[" + node.getIndex() + "]";
    if (node.getKey() != null) return "[" + node.getKey() + "]";
    return "[]";
  }

  /**
   * Every declaration of the violated method in the resource's hierarchy - the generated
   * interface's carries the JAX-RS parameter annotations, the implementation's does not.
   */
  private static List<Method> declarations(Class<?> resource, Path.Node node) {
    if (resource == null) return List.of();
    Class<?>[] types;
    try {
      types = node.as(Path.MethodNode.class).getParameterTypes().toArray(Class<?>[]::new);
    } catch (ClassCastException unsupported) {
      return List.of();
    }
    List<Method> found = new ArrayList<>();
    for (Class<?> type : hierarchy(resource)) {
      try {
        found.add(type.getDeclaredMethod(node.getName(), types));
      } catch (NoSuchMethodException absent) {
        // not declared at this level
      }
    }
    return found;
  }

  private static List<Class<?>> hierarchy(Class<?> type) {
    List<Class<?>> types = new ArrayList<>();
    Deque<Class<?>> pending = new ArrayDeque<>(List.of(type));
    while (!pending.isEmpty()) {
      Class<?> next = pending.removeFirst();
      if (next == Object.class || types.contains(next)) continue;
      types.add(next);
      if (next.getSuperclass() != null) pending.add(next.getSuperclass());
      pending.addAll(List.of(next.getInterfaces()));
    }
    return types;
  }

  /**
   * A header/path/query/... parameter's HTTP name; null for the request body (a parameter no
   * declaration annotates). Falls back to the node's own name only when the method can't be found.
   */
  private static String httpName(List<Method> declarations, Path.Node node) {
    if (declarations.isEmpty()) return node.getName();
    int index;
    try {
      index = node.as(Path.ParameterNode.class).getParameterIndex();
    } catch (ClassCastException unsupported) {
      return node.getName();
    }
    for (Method declaration : declarations) {
      if (index >= 0 && index < declaration.getParameterCount()) {
        String name = httpName(declaration.getParameterAnnotations()[index]);
        if (name != null) return name;
      }
    }
    return null;
  }

  private static String httpName(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation instanceof HeaderParam header) return header.value();
      if (annotation instanceof PathParam path) return path.value();
      if (annotation instanceof QueryParam query) return query.value();
      if (annotation instanceof FormParam form) return form.value();
      if (annotation instanceof CookieParam cookie) return cookie.value();
      if (annotation instanceof MatrixParam matrix) return matrix.value();
    }
    return null;
  }
}
