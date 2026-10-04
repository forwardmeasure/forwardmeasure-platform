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
package com.forwardmeasure.platform.quarkus.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardmeasure.platform.server.jaxrs.RequestProblems;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Sends every 401 Quarkus answers - no token on a path a permission policy protects, or a token
 * that fails validation - as the RFC 9457 problem Spring and Micronaut return, with {@code
 * WWW-Authenticate: Bearer}. Quarkus sends both through {@code HttpAuthenticator.sendChallenge},
 * which asks each mechanism in priority order until one answers; quarkus-oidc's own mechanism
 * (priority {@code quarkus.oidc.priority}, default 1001) answers with an empty body. This one ranks
 * above it and never authenticates anyone, so quarkus-oidc still verifies every token.
 */
@Singleton
public class ProblemChallengeMechanism implements HttpAuthenticationMechanism {
  static final int PRIORITY = 1002;

  private final ObjectMapper mapper;

  @Inject
  public ProblemChallengeMechanism(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      RoutingContext context, IdentityProviderManager identityProviderManager) {
    return Uni.createFrom().nullItem();
  }

  @Override
  public Uni<ChallengeData> getChallenge(RoutingContext context) {
    return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer"));
  }

  @Override
  public Uni<Boolean> sendChallenge(RoutingContext context) {
    String body;
    try {
      body = mapper.writeValueAsString(RequestProblems.httpError(401));
    } catch (JsonProcessingException e) {
      return Uni.createFrom().failure(e);
    }
    context
        .response()
        .setStatusCode(401)
        .putHeader("WWW-Authenticate", "Bearer")
        .putHeader("Content-Type", "application/problem+json")
        .end(body);
    return Uni.createFrom().item(true);
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }
}
