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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.forwardmeasure.openworkflow.common.model.Problem;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QueryParameterRequestFilterTest {
  interface Contract {
    void search(@QueryParam("limit") Integer limit, @QueryParam("ids") List<UUID> ids, String body);
  }

  public static class Parent {
    public void inherited(@QueryParam("value") int value) {}
  }

  public static class Resource extends Parent implements Contract {
    public void search(Integer limit, List<UUID> ids, String body) {}

    public void direct(@DefaultValue("1") @QueryParam("count") long count) {}

    public void header(@HeaderParam("auth") String auth) {}

    public void containers(
        @QueryParam("set") Set<Integer> values, @QueryParam("nested") List<List<String>> nested) {}
  }

  public enum NullableEnum {
    VALUE;

    public static NullableEnum fromValue(String value) {
      return null;
    }
  }

  @Test
  void absentResourceOrUnmatchedMethodDoesNotTouchTheRequest() {
    var filter = new QueryParameterConversionFilter();
    filter.filter(null);
    filter.resource = resource(Resource.class, null);
    filter.filter(null);
  }

  @Test
  void interfaceAnnotationsProduceSortedProblemsForRepeatedAndCommaSeparatedValues()
      throws Exception {
    var query = new MultivaluedHashMap<String, String>();
    query.add("limit", "bad");
    query.add("ids", UUID.randomUUID() + ",not-a-uuid");
    query.add("ids", UUID.randomUUID().toString());
    try (var response =
        filter(
            Resource.class,
            Resource.class.getMethod("search", Integer.class, List.class, String.class),
            query)) {
      assertEquals(400, response.getStatus());
      assertEquals("application/problem+json", response.getMediaType().toString());
      var problem = (Problem) response.getEntity();
      assertEquals(
          List.of("ids", "limit"),
          problem.getViolations().stream().map(v -> v.getField()).toList());
      assertTrue(
          problem.getViolations().stream()
              .allMatch(v -> RequestProblems.INVALID_VALUE.equals(v.getMessage())));
      assertEquals(query.getFirst("ids"), problem.getViolations().getFirst().getRejectedValue());
    }
  }

  @Test
  void acceptsValidAndAbsentValuesAndLeavesNonQueryInputsAlone() throws Exception {
    var method = Resource.class.getMethod("search", Integer.class, List.class, String.class);
    var query = new MultivaluedHashMap<String, String>();
    assertNull(filter(Resource.class, method, query));
    query.add("limit", "32");
    query.add("ids", UUID.randomUUID() + "," + UUID.randomUUID());
    assertNull(filter(Resource.class, method, query));
    assertNull(
        filter(
            Resource.class,
            Resource.class.getMethod("header", String.class),
            new MultivaluedHashMap<>(java.util.Map.of("auth", "not-a-number"))));
  }

  @Test
  void findsDirectAndSuperclassAnnotations() throws Exception {
    var query = new MultivaluedHashMap<String, String>();
    query.add("value", "invalid");
    try (var response =
        filter(Resource.class, Resource.class.getMethod("inherited", int.class), query)) {
      assertEquals("value", ((Problem) response.getEntity()).getViolations().getFirst().getField());
    }
    query.clear();
    query.add("count", "1.5");
    try (var response =
        filter(Resource.class, Resource.class.getMethod("direct", long.class), query)) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void unsupportedGenericShapesRemainTheRuntimesResponsibility() throws Exception {
    var query = new MultivaluedHashMap<String, String>();
    query.add("set", "value");
    query.add("nested", "value");
    assertNull(
        filter(
            Resource.class, Resource.class.getMethod("containers", Set.class, List.class), query));
    assertNull(filter(null, Resource.class.getMethod("header", String.class), query));
  }

  @Test
  void rejectsTrailingEmptyListItemsAndNullableEnumConversions() throws Exception {
    var query = new MultivaluedHashMap<String, String>();
    query.add("ids", UUID.randomUUID() + ",");
    try (var response =
        filter(
            Resource.class,
            Resource.class.getMethod("search", Integer.class, List.class, String.class),
            query)) {
      assertEquals(400, response.getStatus());
    }
    assertFalse(QueryParameterConversionFilter.converts(NullableEnum.class, "unknown"));
    assertTrue(
        QueryParameterConversionFilter.converts(java.time.Instant.class, "2026-10-07T12:00:00Z"));
  }

  private static ResourceInfo resource(Class<?> type, Method method) {
    return new ResourceInfo() {
      public Class<?> getResourceClass() {
        return type;
      }

      public Method getResourceMethod() {
        return method;
      }
    };
  }

  private static Response filter(
      Class<?> type, Method method, MultivaluedMap<String, String> query) {
    var aborted = new java.util.concurrent.atomic.AtomicReference<Response>();
    UriInfo uri =
        (UriInfo)
            Proxy.newProxyInstance(
                UriInfo.class.getClassLoader(),
                new Class<?>[] {UriInfo.class},
                (proxy, called, arguments) -> {
                  if (called.getName().equals("getQueryParameters")) return query;
                  throw new AssertionError("Unexpected URI interaction: " + called);
                });
    ContainerRequestContext request =
        (ContainerRequestContext)
            Proxy.newProxyInstance(
                ContainerRequestContext.class.getClassLoader(),
                new Class<?>[] {ContainerRequestContext.class},
                (proxy, called, arguments) -> {
                  if (called.getName().equals("getUriInfo")) return uri;
                  if (called.getName().equals("abortWith")) {
                    aborted.set((Response) arguments[0]);
                    return null;
                  }
                  throw new AssertionError("Unexpected request interaction: " + called);
                });
    var filter = new QueryParameterConversionFilter();
    filter.resource = resource(type, method);
    filter.filter(request);
    return aborted.get();
  }
}
