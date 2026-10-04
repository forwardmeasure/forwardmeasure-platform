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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * A request property's name as the client writes it in JSON. Bean validation and Micronaut Serde
 * report Java property names; the contract's JSON names differ wherever a model maps one with
 * {@code @JsonProperty} (e.g. {@code displayName} as {@code display_name}).
 */
public final class JsonFieldNames {
  private JsonFieldNames() {}

  /** {@code property}'s JSON name on {@code owner} - its {@code @JsonProperty} value, if any. */
  public static String of(Class<?> owner, String property) {
    if (owner == null || property == null) return property;
    for (AnnotatedElement member : members(owner, property)) {
      JsonProperty json = member.getAnnotation(JsonProperty.class);
      if (json != null && !json.value().isEmpty()) return json.value();
    }
    return property;
  }

  /** {@code property}'s declared type on {@code owner}; null when it can't be found. */
  static Type typeOf(Class<?> owner, String property) {
    if (owner == null || property == null) return null;
    for (AnnotatedElement member : members(owner, property)) {
      if (member instanceof Method getter && getter.getParameterCount() == 0) {
        return getter.getGenericReturnType();
      }
      if (member instanceof Field field) return field.getGenericType();
    }
    return null;
  }

  static Class<?> raw(Type type) {
    if (type instanceof Class<?> type1) return type1;
    if (type instanceof ParameterizedType parameterized) return raw(parameterized.getRawType());
    if (type instanceof GenericArrayType array) {
      Class<?> component = raw(array.getGenericComponentType());
      return component == null ? null : component.arrayType();
    }
    return null;
  }

  /** The element type of a collection or array, or a map's value type; null otherwise. */
  static Type element(Type type) {
    if (type instanceof GenericArrayType array) return array.getGenericComponentType();
    Class<?> raw = raw(type);
    if (raw == null) return null;
    if (raw.isArray()) return raw.getComponentType();
    if (type instanceof ParameterizedType parameterized) {
      Type[] arguments = parameterized.getActualTypeArguments();
      if (Collection.class.isAssignableFrom(raw) && arguments.length == 1) return arguments[0];
      if (Map.class.isAssignableFrom(raw) && arguments.length == 2) return arguments[1];
    }
    return null;
  }

  /** The getter, field and setter backing {@code property}, most specific class first. */
  private static java.util.List<AnnotatedElement> members(Class<?> owner, String property) {
    String capitalized = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    java.util.List<AnnotatedElement> found = new java.util.ArrayList<>();
    for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
      for (Method method : type.getDeclaredMethods()) {
        String name = method.getName();
        if ((name.equals("get" + capitalized) || name.equals("is" + capitalized))
                && method.getParameterCount() == 0
            || name.equals("set" + capitalized) && method.getParameterCount() == 1) {
          found.add(method);
        }
      }
      try {
        found.add(type.getDeclaredField(property));
      } catch (NoSuchFieldException absent) {
        // not declared at this level
      }
    }
    // Getters first, so typeOf sees a getter's declared type before a setter.
    found.sort(
        java.util.Comparator.comparingInt(
            member ->
                member instanceof Method method && method.getParameterCount() == 0
                    ? 0
                    : member instanceof Field ? 1 : 2));
    return found;
  }
}
