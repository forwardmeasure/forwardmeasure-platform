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

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.rules.SecurityRuleResult;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * A bearer token that fails validation is a 401 on every path, open ones included - as on Spring
 * (the resource server rejects any bearer token it can't verify) and Quarkus (proactive
 * authentication). Micronaut security instead treats the request as anonymous, so an {@code
 * isAnonymous()} path (health, an engine's {@code /internal/**}) would let it through. Runs before
 * every other rule; decides nothing for a request without a bearer token or with a valid one.
 */
@Singleton
@Requires(classes = SecurityRule.class)
public class MicronautInvalidBearerTokenRule implements SecurityRule<HttpRequest<?>> {
  private static final String BEARER = "Bearer ";

  @Override
  public Publisher<SecurityRuleResult> check(
      HttpRequest<?> request, Authentication authentication) {
    boolean bearer =
        request != null
            && request
                .getHeaders()
                .getAuthorization()
                .filter(value -> value.regionMatches(true, 0, BEARER, 0, BEARER.length()))
                .isPresent();
    return Mono.just(
        bearer && authentication == null
            ? SecurityRuleResult.REJECTED
            : SecurityRuleResult.UNKNOWN);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
