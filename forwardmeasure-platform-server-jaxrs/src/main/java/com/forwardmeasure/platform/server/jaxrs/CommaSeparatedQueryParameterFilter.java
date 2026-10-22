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

import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the contracts' array query parameters - {@code style: form, explode: false} - as the
 * comma-separated lists the contracts define them as: {@code ?status=OPEN,ASSIGNED} means {@code
 * ?status=OPEN&status=ASSIGNED}. JAX-RS splits only the repeated form, so Quarkus and Jersey
 * convert {@code OPEN,ASSIGNED} as one value, fail, and answer 404 without a log line (JAX-RS's
 * rule for a query parameter it can't convert); Micronaut splits the comma form itself. {@link
 * QueryParameterConversionFilter} validates every item of such a list but cannot split it: only a
 * pre-matching filter may rewrite the request URI, and before matching no resource method is known.
 *
 * <p>So each service names its generated API interfaces in a subclass - annotated {@code @Provider}
 * and {@code @PreMatching} itself, for the runtimes that discover providers - and this filter
 * rewrites exactly those interfaces' {@code List} query parameters, and only when one carries a
 * comma: a comma in any other parameter ({@code correlationId}, say) is data.
 */
@PreMatching
public abstract class CommaSeparatedQueryParameterFilter implements ContainerRequestFilter {
  private final Set<String> arrayParameters;

  /** {@code apis}: the generated interfaces whose {@code List} query parameters are split. */
  protected CommaSeparatedQueryParameterFilter(Class<?>... apis) {
    Set<String> names = new HashSet<>();
    for (Class<?> api : apis) {
      names.addAll(arrayParameters(api));
    }
    this.arrayParameters = Set.copyOf(names);
  }

  @Override
  public void filter(ContainerRequestContext request) {
    MultivaluedMap<String, String> query = request.getUriInfo().getQueryParameters();
    boolean commaSeparated =
        arrayParameters.stream()
            .anyMatch(
                name ->
                    query.getOrDefault(name, List.of()).stream().anyMatch(v -> v.contains(",")));
    if (!commaSeparated) {
      return;
    }
    List<String> pairs = new ArrayList<>();
    for (Map.Entry<String, List<String>> parameter : query.entrySet()) {
      for (String value : parameter.getValue()) {
        List<String> values =
            arrayParameters.contains(parameter.getKey())
                ? List.of(value.split(",", -1))
                : List.of(value);
        for (String item : values) {
          pairs.add(encode(parameter.getKey()) + "=" + encode(item));
        }
      }
    }
    request.setRequestUri(
        UriBuilder.fromUri(request.getUriInfo().getRequestUri())
            .replaceQuery(String.join("&", pairs))
            .build());
  }

  /** The names of the {@code List} query parameters this filter splits. */
  final Set<String> arrayParameters() {
    return arrayParameters;
  }

  /** Percent-encodes a decoded name or value; a space as %20, never the form encoding's '+'. */
  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** Every {@code @QueryParam} of the API declared as a {@code List}. */
  private static Set<String> arrayParameters(Class<?> api) {
    Set<String> names = new HashSet<>();
    for (Method method : api.getMethods()) {
      Class<?>[] types = method.getParameterTypes();
      Annotation[][] annotations = method.getParameterAnnotations();
      for (int index = 0; index < types.length; index++) {
        if (List.class.isAssignableFrom(types[index])) {
          for (Annotation annotation : annotations[index]) {
            if (annotation instanceof QueryParam query) {
              names.add(query.value());
            }
          }
        }
      }
    }
    return names;
  }
}
