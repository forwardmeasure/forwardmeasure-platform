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
package com.forwardmeasure.build.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

final class PolicyInspector {
  private final PlatformPolicy policy;
  private final String consumerGroup;

  PolicyInspector(PlatformPolicy policy) {
    this(policy, null);
  }

  PolicyInspector(PlatformPolicy policy, String consumerGroup) {
    this.policy = policy;
    this.consumerGroup = consumerGroup;
  }

  List<String> inspect(Model model) {
    List<String> violations = new ArrayList<>();
    Map<String, String> exceptions = exceptions(model.getBuild(), violations);
    inspectModel(model, model.getBuild(), exceptions, violations);
    // Inspect even inactive profiles: switching framework/profile must not bypass ownership.
    model
        .getProfiles()
        .forEach(
            profile -> {
              Map<String, String> scoped = new HashMap<>(exceptions);
              scoped.putAll(exceptions(profile.getBuild(), violations));
              inspectModel(profile, profile.getBuild(), scoped, violations);
            });
    return List.copyOf(violations);
  }

  private void inspectModel(
      ModelBase model, BuildBase build, Map<String, String> exceptions, List<String> violations) {
    model.getProperties().stringPropertyNames().stream()
        .sorted()
        .filter(
            property ->
                policy.properties().contains(property)
                    || (property.startsWith("forwardmeasure") && property.endsWith(".version")))
        // CI-friendly reactor parent discovery needs each product root to declare its own revision.
        .filter(property -> !"revision".equals(property))
        .forEach(property -> check("property:" + property, exceptions, violations));
    inspectDependencies(model.getDependencies(), false, exceptions, violations);
    if (model.getDependencyManagement() != null)
      inspectDependencies(
          model.getDependencyManagement().getDependencies(), true, exceptions, violations);
    if (build != null) {
      inspectPlugins(build.getPlugins(), exceptions, violations);
      if (build.getPluginManagement() != null)
        inspectPlugins(build.getPluginManagement().getPlugins(), exceptions, violations);
    }
  }

  private void inspectDependencies(
      List<Dependency> dependencies,
      boolean management,
      Map<String, String> exceptions,
      List<String> violations) {
    for (Dependency dependency : dependencies) {
      String key = PlatformPolicy.dependencyKey(dependency);
      if (dependency.getVersion() == null) continue;
      if ((Objects.equals(consumerGroup, dependency.getGroupId())
              || "${project.groupId}".equals(dependency.getGroupId()))
          && ("${project.version}".equals(dependency.getVersion())
              || "${revision}".equals(dependency.getVersion()))) continue;
      if (!policy.dependencies().containsKey(key)) {
        if (!centrallyVersioned(dependency.getVersion()))
          check("dependency:" + key, exceptions, violations);
        continue;
      }
      // Maven BOM imports require an explicit version. Referencing the platform's own expression
      // preserves ordering without creating an independent version pin.
      if (management
          && ("import".equals(dependency.getScope())
              || !dependency.getExclusions().isEmpty()
              || dependency.getScope() != null)
          && Objects.equals(dependency.getVersion(), policy.dependencies().get(key))) continue;
      check("dependency:" + key, exceptions, violations);
    }
  }

  private void inspectPlugins(
      List<Plugin> plugins, Map<String, String> exceptions, List<String> violations) {
    for (Plugin plugin : plugins) {
      String key = plugin.getGroupId() + ":" + plugin.getArtifactId();
      if (plugin.getVersion() != null
          && (policy.plugins().containsKey(key) || !centrallyVersioned(plugin.getVersion())))
        check("plugin:" + key, exceptions, violations);
    }
  }

  private boolean centrallyVersioned(String version) {
    // Project release identity is allowed only for same-product artifacts in inspectDependencies.
    if ("${project.version}".equals(version) || "${revision}".equals(version)) return false;
    return version.startsWith("${")
        && version.endsWith("}")
        && policy.properties().contains(version.substring(2, version.length() - 1));
  }

  private static void check(String key, Map<String, String> exceptions, List<String> violations) {
    if (!exceptions.containsKey(key))
      violations.add(
          "locally declares platform-owned "
              + key
              + "; inherit it or declare a local override with a reason");
  }

  private static Map<String, String> exceptions(BuildBase build, List<String> violations) {
    Map<String, String> result = new HashMap<>();
    if (build == null) return result;
    // Read only this POM's declaration, not inherited/effective plugin configuration.
    for (Plugin plugin : build.getPlugins()) {
      if (!"com.forwardmeasure.platform".equals(plugin.getGroupId())
          || !"forwardmeasure-platform-policy-maven-plugin".equals(plugin.getArtifactId())
          || !(plugin.getConfiguration() instanceof Xpp3Dom config)) continue;
      Xpp3Dom overrides = config.getChild("overrides");
      if (overrides == null) continue;
      for (Xpp3Dom override : overrides.getChildren("override")) {
        Xpp3Dom keyNode = override.getChild("key");
        Xpp3Dom reasonNode = override.getChild("reason");
        String key = keyNode == null || keyNode.getValue() == null ? "" : keyNode.getValue().trim();
        String reason =
            reasonNode == null || reasonNode.getValue() == null ? "" : reasonNode.getValue().trim();
        if (!key.matches("(property|dependency|plugin):[^\\s*]+") || reason.isBlank()) {
          violations.add("override must specify an exact key and a nonblank reason");
        } else if (key.startsWith("property:forwardmeasure")
            || key.startsWith("dependency:com.forwardmeasure")) {
          violations.add("first-party dependency versions cannot be overridden: " + key);
        } else if (result.putIfAbsent(key, reason) != null) {
          violations.add("duplicate override: " + key);
        }
      }
    }
    return result;
  }
}
