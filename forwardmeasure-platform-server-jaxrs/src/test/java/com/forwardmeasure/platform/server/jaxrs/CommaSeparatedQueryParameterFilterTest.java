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
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * {@link CommaSeparatedQueryParameterFilter}'s rewrite. The services' API contract tests drive it
 * over HTTP on all three frameworks.
 */
final class CommaSeparatedQueryParameterFilterTest {
  interface Tasks {
    void list(
        @QueryParam("status") List<String> status,
        @QueryParam("correlationId") String correlationId,
        @QueryParam("limit") Integer limit);
  }

  interface Executions {
    void list(@QueryParam("state") List<String> state);
  }

  static final class Filter extends CommaSeparatedQueryParameterFilter {
    Filter() {
      super(Tasks.class, Executions.class);
    }
  }

  @Test
  void collectsTheListQueryParametersOfEveryApi() {
    assertEquals(Set.of("status", "state"), new Filter().arrayParameters());
  }

  @Test
  void splitsACommaSeparatedListIntoTheRepeatedForm() {
    URI rewritten =
        filter(
            "http://tasks/v1/human-tasks?status=OPEN%2CASSIGNED%2CCLAIMED&limit=50",
            query("status", "OPEN,ASSIGNED,CLAIMED", "limit", "50"));
    assertEquals(
        Set.of("status=OPEN", "status=ASSIGNED", "status=CLAIMED", "limit=50"), pairs(rewritten));
    assertEquals("/v1/human-tasks", rewritten.getRawPath());
  }

  @Test
  void leavesACommaInAnyOtherParameterAsData() {
    URI rewritten =
        filter(
            "http://tasks/v1/human-tasks?status=OPEN,ASSIGNED&correlationId=a,b",
            query("status", "OPEN,ASSIGNED", "correlationId", "a,b"));
    assertEquals(Set.of("status=OPEN", "status=ASSIGNED", "correlationId=a%2Cb"), pairs(rewritten));
  }

  @Test
  void leavesTheRequestAloneWithoutACommaSeparatedList() {
    assertNull(
        filter(
            "http://tasks/v1/human-tasks?status=OPEN&status=ASSIGNED&correlationId=a,b",
            query("status", "OPEN", "status", "ASSIGNED", "correlationId", "a,b")));
  }

  /** Re-encoded values keep their meaning: a space is %20, never the form encoding's '+'. */
  @Test
  void reEncodesDecodedValuesWithoutTheFormEncoding() {
    URI rewritten =
        filter(
            "http://executions/v1/workflow-executions?state=RUNNING,PAUSED&q=a%20b%2Bc",
            query("state", "RUNNING,PAUSED", "q", "a b+c"));
    assertEquals(Set.of("state=RUNNING", "state=PAUSED", "q=a%20b%2Bc"), pairs(rewritten));
  }

  private static MultivaluedHashMap<String, String> query(String... namesAndValues) {
    var query = new MultivaluedHashMap<String, String>();
    for (int index = 0; index < namesAndValues.length; index += 2) {
      query.add(namesAndValues[index], namesAndValues[index + 1]);
    }
    return query;
  }

  /** The query's name=value pairs; the order of the parameters isn't the filter's to keep. */
  private static Set<String> pairs(URI uri) {
    return Set.of(uri.getRawQuery().split("&"));
  }

  /** Runs the filter; the URI it set, or null when it left the request alone. */
  private static URI filter(String requestUri, MultivaluedHashMap<String, String> query) {
    var rewritten = new AtomicReference<URI>();
    UriInfo uri =
        (UriInfo)
            Proxy.newProxyInstance(
                UriInfo.class.getClassLoader(),
                new Class<?>[] {UriInfo.class},
                (proxy, called, arguments) -> {
                  if (called.getName().equals("getQueryParameters")) return query;
                  if (called.getName().equals("getRequestUri")) return URI.create(requestUri);
                  throw new AssertionError("Unexpected URI interaction: " + called);
                });
    ContainerRequestContext request =
        (ContainerRequestContext)
            Proxy.newProxyInstance(
                ContainerRequestContext.class.getClassLoader(),
                new Class<?>[] {ContainerRequestContext.class},
                (proxy, called, arguments) -> {
                  if (called.getName().equals("getUriInfo")) return uri;
                  if (called.getName().equals("setRequestUri") && arguments.length == 1) {
                    rewritten.set((URI) arguments[0]);
                    return null;
                  }
                  throw new AssertionError("Unexpected request interaction: " + called);
                });
    new Filter().filter(request);
    return rewritten.get();
  }
}
