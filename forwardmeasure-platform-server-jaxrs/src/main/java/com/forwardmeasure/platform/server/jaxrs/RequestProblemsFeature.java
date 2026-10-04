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

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

/**
 * Every mapper this module provides, for a runtime that only sees providers registered explicitly
 * (Spring Boot's Jersey). Not a {@code @Provider} itself: Quarkus discovers the mappers one by one
 * from this module's index, and registering them twice would be ambiguous.
 */
public final class RequestProblemsFeature implements Feature {

  @Override
  public boolean configure(FeatureContext context) {
    context
        .register(ConstraintViolationExceptionMapper.class)
        .register(JsonProcessingProblemMapper.class)
        .register(JsonParseProblemMapper.class)
        .register(JsonMappingProblemMapper.class)
        .register(MismatchedInputProblemMapper.class)
        .register(UnhandledExceptionProblemMapper.class)
        .register(QueryParameterConversionFilter.class);
    return true;
  }
}
