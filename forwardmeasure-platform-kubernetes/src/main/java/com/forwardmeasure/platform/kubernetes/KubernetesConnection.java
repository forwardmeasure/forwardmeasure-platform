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

import io.fabric8.kubernetes.api.model.NamedContext;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * The cluster a service's Kubernetes client talks to, always chosen explicitly: the pod's own
 * service account ({@link InCluster}) or a named kubeconfig file ({@link Kubeconfig}).
 *
 * <p>Never Fabric8's auto-configuration ({@code new KubernetesClientBuilder().build()}, {@code new
 * ConfigBuilder()}): that reads {@code $KUBECONFIG}/{@code ~/.kube/config}, system properties and
 * environment overrides, and runs the kubeconfig's exec credential plugin - so a test on a
 * developer machine reached for the developer's production cluster, and production code could act
 * on whichever cluster the JVM happened to see. Every client here is built on {@link
 * Config#empty()} or from the one named file, so nothing ambient is consulted, and a missing or
 * inconsistent setting fails at startup instead of falling back to anything.
 */
public sealed interface KubernetesConnection
    permits KubernetesConnection.InCluster, KubernetesConnection.Kubeconfig {

  /** Setting value selecting {@link InCluster}. */
  String IN_CLUSTER = "in-cluster";

  /** Setting value selecting {@link Kubeconfig}. */
  String KUBECONFIG = "kubeconfig";

  /** Where the kubelet mounts the pod's service-account token, CA and namespace. */
  Path POD_SERVICE_ACCOUNT_DIRECTORY = Path.of("/var/run/secrets/kubernetes.io/serviceaccount");

  /**
   * The connection a service's settings select. Every framework binding reads its three settings
   * and passes them here, so all services accept the same values and fail the same way.
   *
   * @param settingPrefix the settings' common prefix, named in every error (e.g. {@code
   *     entity-intelligence.kubernetes})
   * @param mode {@value #IN_CLUSTER} or {@value #KUBECONFIG}; required
   * @param kubeconfig the kubeconfig file; required for {@value #KUBECONFIG}, rejected otherwise
   * @param context the kubeconfig context; optional for {@value #KUBECONFIG} (the file's own
   *     current-context), rejected otherwise
   */
  static KubernetesConnection fromSettings(
      String settingPrefix, String mode, String kubeconfig, String context) {
    Objects.requireNonNull(settingPrefix, "settingPrefix");
    String modeSetting = settingPrefix + ".connection";
    String kubeconfigSetting = settingPrefix + ".kubeconfig";
    String contextSetting = settingPrefix + ".context";
    if (isBlank(mode)) {
      throw new IllegalStateException(
          modeSetting
              + " is required: '"
              + IN_CLUSTER
              + "' (the pod's service account) or '"
              + KUBECONFIG
              + "' (with "
              + kubeconfigSetting
              + ")");
    }
    return switch (mode.trim()) {
      case IN_CLUSTER -> {
        if (!isBlank(kubeconfig) || !isBlank(context)) {
          throw new IllegalStateException(
              modeSetting
                  + "="
                  + IN_CLUSTER
                  + " takes no "
                  + kubeconfigSetting
                  + " or "
                  + contextSetting);
        }
        yield inCluster();
      }
      case KUBECONFIG -> {
        if (isBlank(kubeconfig)) {
          throw new IllegalStateException(
              kubeconfigSetting + " is required when " + modeSetting + "=" + KUBECONFIG);
        }
        yield new Kubeconfig(Path.of(kubeconfig.trim()), isBlank(context) ? null : context.trim());
      }
      default ->
          throw new IllegalStateException(
              modeSetting
                  + " must be '"
                  + IN_CLUSTER
                  + "' or '"
                  + KUBECONFIG
                  + "', not '"
                  + mode
                  + "'");
    };
  }

  /** The pod's own service account and API server, as the kubelet provides them. */
  static InCluster inCluster() {
    return InCluster.fromPod(System.getenv(), POD_SERVICE_ACCOUNT_DIRECTORY);
  }

  /** The named kubeconfig file, at its own current-context. */
  static Kubeconfig kubeconfig(Path file) {
    return new Kubeconfig(file, null);
  }

  /** The named kubeconfig file, at the named context. */
  static Kubeconfig kubeconfig(Path file, String context) {
    return new Kubeconfig(file, Objects.requireNonNull(context, "context"));
  }

  /** The Fabric8 configuration for this connection, built without any auto-configuration. */
  Config config();

  /** A new client for this connection; the caller owns and closes it. */
  default KubernetesClient newClient() {
    return new KubernetesClientBuilder().withConfig(config()).build();
  }

  /**
   * The pod's own service account: the API server the kubelet advertises and the token, CA and
   * namespace it mounts. The token is re-read on every request, so the kubelet's rotation of the
   * projected token is picked up without rebuilding the client.
   */
  record InCluster(URI apiServer, Path serviceAccountDirectory) implements KubernetesConnection {

    public InCluster {
      Objects.requireNonNull(apiServer, "apiServer");
      Objects.requireNonNull(serviceAccountDirectory, "serviceAccountDirectory");
      for (String file : new String[] {"token", "ca.crt", "namespace"}) {
        Path path = serviceAccountDirectory.resolve(file);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
          throw new IllegalStateException(
              "In-cluster Kubernetes connection: service-account file " + path + " is missing");
        }
      }
    }

    /**
     * The API server from the kubelet's {@code KUBERNETES_SERVICE_HOST}/{@code
     * KUBERNETES_SERVICE_PORT}; outside a pod they are absent and this fails.
     */
    static InCluster fromPod(Map<String, String> environment, Path serviceAccountDirectory) {
      String host = environment.get("KUBERNETES_SERVICE_HOST");
      String port = environment.get("KUBERNETES_SERVICE_PORT");
      if (isBlank(host) || isBlank(port)) {
        throw new IllegalStateException(
            "In-cluster Kubernetes connection: KUBERNETES_SERVICE_HOST/KUBERNETES_SERVICE_PORT"
                + " are not set - this process is not running in a Kubernetes pod");
      }
      String authority = host.contains(":") ? "[" + host + "]" : host;
      return new InCluster(
          URI.create("https://" + authority + ":" + port.trim()), serviceAccountDirectory);
    }

    /** The pod's own namespace, from the mounted service account. */
    public String namespace() {
      return read(serviceAccountDirectory.resolve("namespace"));
    }

    @Override
    public Config config() {
      Path token = serviceAccountDirectory.resolve("token");
      return new ConfigBuilder(Config.empty())
          .withMasterUrl(apiServer.toString())
          .withCaCertFile(serviceAccountDirectory.resolve("ca.crt").toString())
          .withNamespace(namespace())
          .withOauthTokenProvider(() -> read(token))
          .build();
    }
  }

  /**
   * A named kubeconfig file, at a named context or the file's own current-context. The context is
   * checked before Fabric8 sees the file: Fabric8 silently falls back to the current-context (or to
   * {@code https://kubernetes.default.svc}) when the requested one is absent, and runs the fallback
   * user's exec credential plugin while doing so.
   */
  record Kubeconfig(Path file, String context) implements KubernetesConnection {

    public Kubeconfig {
      Objects.requireNonNull(file, "file");
      if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
        throw new IllegalStateException("Kubeconfig " + file + " does not exist or is unreadable");
      }
      if (context != null && context.isBlank()) {
        throw new IllegalArgumentException("Kubeconfig context must not be blank");
      }
    }

    /** The context this connection uses: the named one, or the file's own current-context. */
    public String effectiveContext() {
      io.fabric8.kubernetes.api.model.Config parsed;
      try {
        parsed =
            new KubernetesSerialization()
                .unmarshal(read(file), io.fabric8.kubernetes.api.model.Config.class);
      } catch (RuntimeException failure) {
        throw new IllegalStateException(
            "Kubeconfig " + file + " is not a valid kubeconfig", failure);
      }
      String selected = context != null ? context : parsed.getCurrentContext();
      if (isBlank(selected)) {
        throw new IllegalStateException(
            "Kubeconfig " + file + " has no current-context; name the context explicitly");
      }
      boolean defined =
          parsed.getContexts() != null
              && parsed.getContexts().stream()
                  .map(NamedContext::getName)
                  .anyMatch(selected::equals);
      if (!defined) {
        throw new IllegalStateException(
            "Kubeconfig " + file + " defines no context '" + selected + "'");
      }
      return selected;
    }

    @Override
    public Config config() {
      return Config.fromKubeconfig(effectiveContext(), file.toFile());
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String read(Path file) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8).trim();
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read " + file, failure);
    }
  }
}
