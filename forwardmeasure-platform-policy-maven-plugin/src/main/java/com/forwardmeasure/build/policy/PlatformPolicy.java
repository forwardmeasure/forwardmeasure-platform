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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

/** Ownership is derived from the actual platform ancestor, never a hand-maintained allowlist. */
record PlatformPolicy(
    Set<String> properties, Map<String, String> dependencies, Map<String, String> plugins) {
  static PlatformPolicy from(Model platform) {
    Map<String, String> dependencies = new TreeMap<>();
    if (platform.getDependencyManagement() != null) {
      platform
          .getDependencyManagement()
          .getDependencies()
          .forEach(d -> dependencies.put(dependencyKey(d), d.getVersion()));
    }
    Map<String, String> plugins = new TreeMap<>();
    if (platform.getBuild() != null) {
      if (platform.getBuild().getPluginManagement() != null) {
        platform
            .getBuild()
            .getPluginManagement()
            .getPlugins()
            .forEach(p -> plugins.put(p.getGroupId() + ":" + p.getArtifactId(), p.getVersion()));
      }
      platform
          .getBuild()
          .getPlugins()
          .forEach(
              p -> {
                if (p.getVersion() != null)
                  plugins.put(p.getGroupId() + ":" + p.getArtifactId(), p.getVersion());
              });
    }
    return new PlatformPolicy(
        Set.copyOf(platform.getProperties().stringPropertyNames()),
        Map.copyOf(dependencies),
        Map.copyOf(plugins));
  }

  static String dependencyKey(Dependency dependency) {
    String base = dependency.getGroupId() + ":" + dependency.getArtifactId();
    String classifier = dependency.getClassifier();
    String type = dependency.getType();
    if ((type == null || "jar".equals(type)) && (classifier == null || classifier.isEmpty()))
      return base;
    return base + ":" + type + ":" + (classifier == null ? "" : classifier);
  }
}
