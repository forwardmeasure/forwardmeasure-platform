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
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;

final class PolicyInspector {
  List<String> inspect(Model model) {
    List<String> violations = new ArrayList<>();
    PlatformPolicy.MANAGED_PROPERTIES.stream()
        .filter(model.getProperties()::containsKey)
        .sorted()
        .forEach(
            property ->
                violations.add("locally declares platform-owned property '" + property + "'"));
    inspectDependencies(model.getDependencies(), violations);
    DependencyManagement dependencyManagement = model.getDependencyManagement();
    if (dependencyManagement != null) {
      inspectDependencies(dependencyManagement.getDependencies(), violations);
    }
    Build build = model.getBuild();
    if (build != null) {
      inspectPlugins(build.getPlugins(), violations);
      PluginManagement pluginManagement = build.getPluginManagement();
      if (pluginManagement != null) {
        inspectPlugins(pluginManagement.getPlugins(), violations);
      }
    }
    return List.copyOf(violations);
  }

  private static void inspectDependencies(List<Dependency> dependencies, List<String> violations) {
    dependencies.stream()
        .filter(dependency -> dependency.getVersion() != null)
        .filter(
            dependency ->
                PlatformPolicy.MANAGED_DEPENDENCIES.contains(
                    dependency.getGroupId() + ":" + dependency.getArtifactId()))
        .forEach(
            dependency ->
                violations.add(
                    "sets a version on platform-owned dependency '"
                        + dependency.getGroupId()
                        + ":"
                        + dependency.getArtifactId()
                        + "'"));
  }

  private static void inspectPlugins(List<Plugin> plugins, List<String> violations) {
    plugins.stream()
        .filter(plugin -> plugin.getVersion() != null)
        .filter(
            plugin ->
                PlatformPolicy.MANAGED_PLUGINS.contains(
                    plugin.getGroupId() + ":" + plugin.getArtifactId()))
        .forEach(
            plugin ->
                violations.add(
                    "sets a version on platform-owned plugin '"
                        + plugin.getGroupId()
                        + ":"
                        + plugin.getArtifactId()
                        + "'"));
  }
}
