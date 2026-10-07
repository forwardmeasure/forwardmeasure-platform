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

import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/** A root-level aggregate preference is not evidence that an aggregate actually checks a module. */
final class CoverageOwnership {
  static final String SKIP_PROPERTY = "forwardmeasure.coverage.module.skip";

  static boolean owned(MavenProject module, List<MavenProject> reactor) {
    return reactor.stream()
        .anyMatch(
            candidate ->
                isAggregate(candidate)
                    && candidate.getDependencies().stream()
                        .anyMatch(
                            dependency ->
                                module.getGroupId().equals(dependency.getGroupId())
                                    && module.getArtifactId().equals(dependency.getArtifactId())
                                    && module.getVersion().equals(dependency.getVersion())
                                    && !"test".equals(dependency.getScope()))
                    && candidate.getBuildPlugins().stream()
                        .filter(plugin -> "maven-dependency-plugin".equals(plugin.getArtifactId()))
                        .flatMap(plugin -> plugin.getExecutions().stream())
                        .anyMatch(
                            execution ->
                                execution.getGoals().contains("unpack-dependencies")
                                    && stages(module, (Xpp3Dom) execution.getConfiguration())));
  }

  static boolean isAggregate(MavenProject project) {
    return project.getBuildPlugins().stream()
        .filter(
            plugin ->
                "org.jacoco".equals(plugin.getGroupId())
                    && "jacoco-maven-plugin".equals(plugin.getArtifactId()))
        .anyMatch(plugin -> goal(plugin, "report-aggregate") && goal(plugin, "check"));
  }

  private static boolean goal(Plugin plugin, String goal) {
    return plugin.getExecutions().stream()
        .anyMatch(execution -> execution.getGoals().contains(goal));
  }

  private static boolean stages(MavenProject module, Xpp3Dom configuration) {
    if (configuration == null) return false;
    String includes = value(configuration, "includeArtifactIds");
    String groups = value(configuration, "includeGroupIds");
    return contains(includes, module.getArtifactId())
        && (groups.isBlank() || contains(groups, module.getGroupId()))
        && !contains(value(configuration, "excludeArtifactIds"), module.getArtifactId())
        && !contains(value(configuration, "excludeGroupIds"), module.getGroupId());
  }

  private static String value(Xpp3Dom config, String name) {
    var child = config.getChild(name);
    return child == null || child.getValue() == null ? "" : child.getValue();
  }

  private static boolean contains(String csv, String value) {
    return Arrays.stream(csv.split(",")).map(String::trim).anyMatch(value::equals);
  }
}
