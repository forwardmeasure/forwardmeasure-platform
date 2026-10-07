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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardmeasure.openworkflow.common.model.Problem;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SecurityResponseContractTest {
  @Test
  void missingAndInvalidCredentialsHaveIdenticalSafeChallenges() {
    try (var missing = new UnauthorizedProblemMapper().toResponse(new UnauthorizedException());
        var invalid =
            new AuthenticationFailedProblemMapper()
                .toResponse(
                    new AuthenticationFailedException("private token verification details"))) {
      assertProblem(missing, 401, "Unauthorized");
      assertProblem(invalid, 401, "Unauthorized");
      assertEquals("Bearer", missing.getHeaderString("WWW-Authenticate"));
      assertEquals("Bearer", invalid.getHeaderString("WWW-Authenticate"));
    }
  }

  @Test
  void authorizationFailureDoesNotAskAuthenticatedCallerToAuthenticateAgain() {
    try (var response = new ForbiddenProblemMapper().toResponse(new ForbiddenException())) {
      assertProblem(response, 403, "Forbidden");
      assertNull(response.getHeaderString("WWW-Authenticate"));
    }
  }

  @Test
  void httpLayerChallengeWritesActualJsonResponseAndNeverAuthenticatesCaller() throws Exception {
    var mechanism = new ProblemChallengeMechanism(new ObjectMapper());
    assertNull(mechanism.authenticate(null, null).await().atMost(Duration.ofSeconds(2)));
    var challenge = mechanism.getChallenge(null).await().atMost(Duration.ofSeconds(2));
    assertEquals(401, challenge.status);
    assertEquals("Bearer", challenge.getHeaders().get("WWW-Authenticate"));
    assertTrue(mechanism.getPriority() > 1001, "Must precede the default OIDC challenge");
    var vertx = Vertx.vertx();
    try {
      var router = Router.router(vertx);
      router
          .get("/protected")
          .handler(
              ctx ->
                  mechanism
                      .sendChallenge(ctx)
                      .subscribe()
                      .with(
                          sent -> {
                            if (!sent) ctx.fail(500);
                          },
                          ctx::fail));
      var server =
          vertx
              .createHttpServer()
              .requestHandler(router)
              .listen(0, "127.0.0.1")
              .toCompletionStage()
              .toCompletableFuture()
              .get(10, TimeUnit.SECONDS);
      try (var client = HttpClient.newHttpClient()) {
        var response =
            client.send(
                HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + server.actualPort() + "/protected"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertEquals("Bearer", response.headers().firstValue("WWW-Authenticate").orElseThrow());
        assertEquals(
            "application/problem+json",
            response.headers().firstValue("Content-Type").orElseThrow());
        var body = new ObjectMapper().readTree(response.body());
        assertEquals("about:blank", body.path("type").asText());
        assertEquals(401, body.path("status").asInt());
        assertEquals("HTTP 401 Unauthorized", body.path("detail").asText());
      }
    } finally {
      vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }
  }

  private static void assertProblem(Response response, int status, String title) {
    assertEquals(status, response.getStatus());
    assertEquals("application/problem+json", response.getMediaType().toString());
    var body = (Problem) response.getEntity();
    assertEquals("about:blank", body.getType());
    assertEquals(status, body.getStatus());
    assertEquals(title, body.getTitle());
    assertEquals("HTTP " + status + " " + title, body.getDetail());
  }
}
