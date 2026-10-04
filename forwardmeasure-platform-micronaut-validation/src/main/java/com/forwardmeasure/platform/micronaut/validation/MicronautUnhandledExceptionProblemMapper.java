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

import com.forwardmeasure.platform.server.jaxrs.UnhandledExceptionProblemMapper;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;

/**
 * Micronaut compile-time discovery edge for {@link UnhandledExceptionProblemMapper} - a JAX-RS
 * mapper in a framework-neutral module is invisible to micronaut-jaxrs's exception handlers unless
 * a bean compiled with micronaut-jaxrs-processor stands in for it. {@code @Priority} is redeclared
 * because it isn't inherited.
 *
 * <p>micronaut-jaxrs remaps Micronaut's own routing failures (no such path, unacceptable or
 * unsupported media type) to their JAX-RS equivalents but keeps Micronaut's message; the problem
 * uses the message JAX-RS runtimes give the same failure instead.
 */
@Singleton
@Priority(Integer.MAX_VALUE)
public class MicronautUnhandledExceptionProblemMapper extends UnhandledExceptionProblemMapper {

  @Override
  protected String clientErrorDetail(WebApplicationException exception) {
    return exception.getCause() instanceof HttpStatusException
        ? null
        : super.clientErrorDetail(exception);
  }
}
