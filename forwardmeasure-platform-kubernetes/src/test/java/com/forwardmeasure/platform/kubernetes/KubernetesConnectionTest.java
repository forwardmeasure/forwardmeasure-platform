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
package com.forwardmeasure.platform.kubernetes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.fabric8.kubernetes.client.Config;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Each connection drives a real Fabric8 client over HTTP against local API-server fixtures, so the
 * assertions are on the requests the client actually sent: which server, which path (namespace) and
 * which bearer token.
 */
final class KubernetesConnectionTest {

  /** A throwaway self-signed CA; only parsed, never used to verify a peer. */
  private static final String TEST_CA =
      """
      -----BEGIN CERTIFICATE-----
      MIIBwTCCAWegAwIBAgIUSo0sZchMPoNYpNtHYc1SOwEitvcwCgYIKoZIzj0EAwIw
      NTEzMDEGA1UEAwwqZm9yd2FyZG1lYXN1cmUtcGxhdGZvcm0ta3ViZXJuZXRlcy10
      ZXN0LWNhMCAXDTI2MTAyMDE3NDIwNloYDzIxMjYwOTI2MTc0MjA2WjA1MTMwMQYD
      VQQDDCpmb3J3YXJkbWVhc3VyZS1wbGF0Zm9ybS1rdWJlcm5ldGVzLXRlc3QtY2Ew
      WTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARrLTPg9lqW75JGJlKEpiLc0rSM6gwM
      L9AeLGd1/ryq66yGMUI/FjKDYLv7+s0ftaDwXrGHM7rrj4ZNHJwlSNrro1MwUTAd
      BgNVHQ4EFgQUuOH3wlS01yQnhpTq5hRlenCRYqgwHwYDVR0jBBgwFoAUuOH3wlS0
      1yQnhpTq5hRlenCRYqgwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNIADBF
      AiEA+X8yochAFTe9Lo2n0HXAFqDhEM/1AxTIFUAN3XjiqJ0CICvOthUdd3aouu0u
      wNHUor0HMDyKfbD8F7PD04itmrMV
      -----END CERTIFICATE-----
      """;

  @TempDir Path directory;

  private ApiServer first;
  private ApiServer second;
  private ApiServer ambient;

  @BeforeEach
  void start() throws IOException {
    first = new ApiServer();
    second = new ApiServer();
    ambient = new ApiServer();
  }

  @AfterEach
  void stop() {
    first.close();
    second.close();
    ambient.close();
  }

  @Test
  void kubeconfigUsesTheFilesOwnCurrentContext() throws IOException {
    var connection = KubernetesConnection.kubeconfig(twoContextKubeconfig());

    listPods(connection);

    assertEquals(List.of("/api/v1/namespaces/first-ns/pods Bearer first-token"), first.requests);
    assertEquals(List.of(), second.requests);
  }

  @Test
  void kubeconfigUsesTheNamedContext() throws IOException {
    var connection = KubernetesConnection.kubeconfig(twoContextKubeconfig(), "second");

    listPods(connection);

    assertEquals(List.of(), first.requests);
    assertEquals(List.of("/api/v1/namespaces/second-ns/pods Bearer second-token"), second.requests);
  }

  @Test
  void anUnknownContextFailsInsteadOfFallingBackToTheCurrentOne() throws IOException {
    var connection = KubernetesConnection.kubeconfig(twoContextKubeconfig(), "missing");

    var failure = assertThrows(IllegalStateException.class, connection::config);

    assertTrue(failure.getMessage().contains("defines no context 'missing'"), failure.getMessage());
    assertEquals(List.of(), first.requests);
    assertEquals(List.of(), second.requests);
  }

  @Test
  void aKubeconfigWithoutCurrentContextNeedsTheContextNamed() throws IOException {
    Path file =
        Files.writeString(
            directory.resolve("no-current"),
            twoContextKubeconfigText().replace("current-context: first\n", ""));

    var failure =
        assertThrows(
            IllegalStateException.class, () -> KubernetesConnection.kubeconfig(file).config());

    assertTrue(failure.getMessage().contains("has no current-context"), failure.getMessage());
  }

  @Test
  void aMissingKubeconfigFileFailsAtConstruction() {
    var failure =
        assertThrows(
            IllegalStateException.class,
            () -> KubernetesConnection.kubeconfig(directory.resolve("absent")));

    assertTrue(failure.getMessage().contains("does not exist"), failure.getMessage());
  }

  @Test
  void inClusterUsesTheServiceAccountAndRereadsTheRotatedToken() throws IOException {
    Path account = serviceAccount("first-pod-token", "pod-ns");
    var connection = new KubernetesConnection.InCluster(first.uri(), account);

    try (var client = connection.newClient()) {
      client.pods().list();
      Files.writeString(account.resolve("token"), "rotated-pod-token\n");
      client.pods().list();
    }

    assertEquals(
        List.of(
            "/api/v1/namespaces/pod-ns/pods Bearer first-pod-token",
            "/api/v1/namespaces/pod-ns/pods Bearer rotated-pod-token"),
        first.requests);
    assertEquals("pod-ns", connection.namespace());
  }

  @Test
  void ambientConfigurationIsNeverConsulted() throws IOException {
    Path ambientKubeconfig =
        Files.writeString(
            directory.resolve("ambient"),
            """
            apiVersion: v1
            kind: Config
            current-context: ambient
            clusters:
            - name: ambient
              cluster: {server: "%s"}
            users:
            - name: ambient-user
              user: {token: ambient-token}
            contexts:
            - name: ambient
              context: {cluster: ambient, user: ambient-user, namespace: ambient-ns}
            """
                .formatted(ambient.uri()));
    Map<String, String> ambientProperties =
        Map.of(
            Config.KUBERNETES_KUBECONFIG_FILE,
            ambientKubeconfig.toString(),
            Config.KUBERNETES_MASTER_SYSTEM_PROPERTY,
            ambient.uri().toString(),
            Config.KUBERNETES_OAUTH_TOKEN_SYSTEM_PROPERTY,
            "ambient-token",
            Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY,
            "ambient-ns");
    var previous = new java.util.HashMap<String, String>();
    ambientProperties.forEach((key, value) -> previous.put(key, System.setProperty(key, value)));
    try {
      listPods(KubernetesConnection.kubeconfig(twoContextKubeconfig()));
      listPods(
          new KubernetesConnection.InCluster(second.uri(), serviceAccount("pod-token", "pod-ns")));
    } finally {
      previous.forEach(
          (key, value) -> {
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
          });
    }

    assertEquals(List.of("/api/v1/namespaces/first-ns/pods Bearer first-token"), first.requests);
    assertEquals(List.of("/api/v1/namespaces/pod-ns/pods Bearer pod-token"), second.requests);
    assertEquals(List.of(), ambient.requests);
  }

  @Test
  void inClusterOutsideAPodFails() throws IOException {
    Path account = serviceAccount("token", "ns");

    var failure =
        assertThrows(
            IllegalStateException.class,
            () -> KubernetesConnection.InCluster.fromPod(Map.of(), account));

    assertTrue(failure.getMessage().contains("not running in a Kubernetes pod"));
  }

  @Test
  void inClusterTakesTheApiServerTheKubeletAdvertises() throws IOException {
    Path account = serviceAccount("token", "ns");

    var ipv4 =
        KubernetesConnection.InCluster.fromPod(
            Map.of("KUBERNETES_SERVICE_HOST", "10.0.0.1", "KUBERNETES_SERVICE_PORT", "443"),
            account);
    var ipv6 =
        KubernetesConnection.InCluster.fromPod(
            Map.of("KUBERNETES_SERVICE_HOST", "fd00::1", "KUBERNETES_SERVICE_PORT", "6443"),
            account);

    assertEquals(URI.create("https://10.0.0.1:443"), ipv4.apiServer());
    assertEquals(URI.create("https://[fd00::1]:6443"), ipv6.apiServer());
  }

  @Test
  void inClusterWithoutAMountedServiceAccountFails() throws IOException {
    Path account = serviceAccount("token", "ns");
    Files.delete(account.resolve("ca.crt"));

    var failure =
        assertThrows(
            IllegalStateException.class,
            () -> new KubernetesConnection.InCluster(first.uri(), account));

    assertTrue(failure.getMessage().contains("ca.crt is missing"), failure.getMessage());
  }

  @Test
  void settingsSelectAKubeconfigAndContext() throws IOException {
    Path file = twoContextKubeconfig();

    var connection =
        KubernetesConnection.fromSettings(
            "svc.kubernetes", "kubeconfig", file.toString(), "second");
    var withoutContext =
        KubernetesConnection.fromSettings("svc.kubernetes", " kubeconfig ", file.toString(), "");

    var kubeconfig = assertInstanceOf(KubernetesConnection.Kubeconfig.class, connection);
    assertEquals(file, kubeconfig.file());
    assertEquals("second", kubeconfig.context());
    assertNull(((KubernetesConnection.Kubeconfig) withoutContext).context());
  }

  @Test
  void settingsWithoutAModeFailNamingTheSetting() {
    var failure =
        assertThrows(
            IllegalStateException.class,
            () -> KubernetesConnection.fromSettings("svc.kubernetes", null, null, null));

    assertTrue(
        failure.getMessage().startsWith("svc.kubernetes.connection is required"),
        failure.getMessage());
  }

  @Test
  void settingsWithAnUnknownModeFail() {
    var failure =
        assertThrows(
            IllegalStateException.class,
            () -> KubernetesConnection.fromSettings("svc.kubernetes", "default", null, null));

    assertTrue(failure.getMessage().contains("not 'default'"), failure.getMessage());
  }

  @Test
  void kubeconfigSettingsRequireTheFile() {
    var failure =
        assertThrows(
            IllegalStateException.class,
            () -> KubernetesConnection.fromSettings("svc.kubernetes", "kubeconfig", " ", null));

    assertTrue(
        failure.getMessage().startsWith("svc.kubernetes.kubeconfig is required"),
        failure.getMessage());
  }

  @Test
  void inClusterSettingsRejectAKubeconfig() {
    var failure =
        assertThrows(
            IllegalStateException.class,
            () ->
                KubernetesConnection.fromSettings(
                    "svc.kubernetes", "in-cluster", "/some/kubeconfig", null));

    assertTrue(failure.getMessage().contains("takes no svc.kubernetes.kubeconfig"));
  }

  private static void listPods(KubernetesConnection connection) {
    try (var client = connection.newClient()) {
      client.pods().list();
    }
  }

  private Path twoContextKubeconfig() throws IOException {
    return Files.writeString(directory.resolve("kubeconfig"), twoContextKubeconfigText());
  }

  private String twoContextKubeconfigText() {
    return """
    apiVersion: v1
    kind: Config
    current-context: first
    clusters:
    - name: first
      cluster: {server: "%s"}
    - name: second
      cluster: {server: "%s"}
    users:
    - name: first-user
      user: {token: first-token}
    - name: second-user
      user: {token: second-token}
    contexts:
    - name: first
      context: {cluster: first, user: first-user, namespace: first-ns}
    - name: second
      context: {cluster: second, user: second-user, namespace: second-ns}
    """
        .formatted(first.uri(), second.uri());
  }

  private Path serviceAccount(String token, String namespace) throws IOException {
    Path account = Files.createTempDirectory(directory, "serviceaccount");
    Files.writeString(account.resolve("token"), token + "\n");
    Files.writeString(account.resolve("ca.crt"), TEST_CA);
    Files.writeString(account.resolve("namespace"), namespace);
    return account;
  }

  /** A local API server that answers every list with an empty one and records each request. */
  private static final class ApiServer implements AutoCloseable {
    private final HttpServer server;
    final List<String> requests = new CopyOnWriteArrayList<>();

    ApiServer() throws IOException {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            requests.add(
                exchange.getRequestURI().getPath()
                    + " "
                    + exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body =
                "{\"apiVersion\":\"v1\",\"kind\":\"PodList\",\"metadata\":{},\"items\":[]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      server.start();
    }

    URI uri() {
      return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}
