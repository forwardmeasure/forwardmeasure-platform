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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.rules.SecurityRuleResult;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class MicronautSecurityProblemTest {
  @Test
  void invalidBearerTokensCannotFallThroughToAnonymousAccessOnPublicRoutes() {
    var rule = new MicronautInvalidBearerTokenRule();
    for (String path : java.util.List.of("/health", "/internal/jobs", "/api/entities")) {
      var request = HttpRequest.GET(path).header(HttpHeaders.AUTHORIZATION, "bEaReR invalid-token");
      assertEquals(
          SecurityRuleResult.REJECTED,
          Mono.from(rule.check(request, null)).block(Duration.ofSeconds(2)));
      assertEquals(
          SecurityRuleResult.UNKNOWN,
          Mono.from(rule.check(request, Authentication.build("verified-user")))
              .block(Duration.ofSeconds(2)));
    }
    assertEquals(Ordered.HIGHEST_PRECEDENCE, rule.getOrder());
  }

  @Test
  void absentAndNonBearerCredentialsLeaveTheDecisionToOtherRules() {
    var rule = new MicronautInvalidBearerTokenRule();
    assertEquals(
        SecurityRuleResult.UNKNOWN, Mono.from(rule.check(null, null)).block(Duration.ofSeconds(2)));
    assertEquals(
        SecurityRuleResult.UNKNOWN,
        Mono.from(rule.check(HttpRequest.GET("/health"), null)).block(Duration.ofSeconds(2)));
    assertEquals(
        SecurityRuleResult.UNKNOWN,
        Mono.from(
                rule.check(
                    HttpRequest.GET("/health")
                        .header(HttpHeaders.AUTHORIZATION, "Basic credentials"),
                    null))
            .block(Duration.ofSeconds(2)));
  }

  @Test
  void authenticationFailuresChallengeButAuthorizationFailuresDoNot() {
    var handler = new MicronautAuthorizationProblemHandler();
    var request = HttpRequest.GET("/api/entities");
    var unauthorized = handler.handle(request, new AuthorizationException(null));
    assertEquals(401, unauthorized.code());
    assertEquals("Bearer", unauthorized.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE));
    assertEquals(
        "application/problem+json", unauthorized.getContentType().orElseThrow().toString());
    assertEquals(401, unauthorized.body().getStatus());
    assertEquals("Unauthorized", unauthorized.body().getTitle());
    var forbidden =
        handler.handle(
            request, new AuthorizationException(Authentication.build("authenticated-user")));
    assertEquals(403, forbidden.code());
    assertNull(forbidden.getHeaders().get(HttpHeaders.WWW_AUTHENTICATE));
    assertEquals("Forbidden", forbidden.body().getTitle());
  }
}
