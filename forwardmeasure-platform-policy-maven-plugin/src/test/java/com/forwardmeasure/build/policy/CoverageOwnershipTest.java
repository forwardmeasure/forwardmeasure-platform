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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

class CoverageOwnershipTest {
  @Test
  void aReportAndStagedDependencyWithAnActualCheckOwnTheModule() {
    var module = module("worker");
    var owner = aggregate(module);
    assertTrue(CoverageOwnership.owned(module, List.of(module, owner)));
    assertFalse(CoverageOwnership.owned(module("other"), List.of(owner)));
    assertFalse(CoverageOwnership.owned(module, List.of(module)));
  }

  @Test
  void reportingOnlyAndTestScopeDependenciesCannotDisableModuleGates() {
    var module = module("worker");
    var owner = aggregate(module);
    owner.getDependencies().getFirst().setScope("test");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    owner.getDependencies().getFirst().setScope("compile");
    var jacoco = owner.getBuildPlugins().getFirst();
    jacoco.getExecutions().getFirst().getGoals().remove("check");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    jacoco.getExecutions().getFirst().addGoal("check");
    jacoco.getExecutions().getFirst().getGoals().remove("report-aggregate");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
  }

  @Test
  void unstagedOrExplicitlyExcludedModulesRetainTheirOwnGate() {
    var module = module("worker");
    var owner = aggregate(module);
    var stage = owner.getBuildPlugins().get(1).getExecutions().getFirst();
    var config = (Xpp3Dom) stage.getConfiguration();
    config.getChild("includeArtifactIds").setValue("other");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    config.getChild("includeArtifactIds").setValue(" other, worker ");
    child(config, "excludeArtifactIds", "worker");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    config.getChild("excludeArtifactIds").setValue("");
    child(config, "includeGroupIds", "wrong");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    config.getChild("includeGroupIds").setValue("test");
    child(config, "excludeGroupIds", "test");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    config.getChild("excludeGroupIds").setValue("");
    assertTrue(CoverageOwnership.owned(module, List.of(owner)));
    stage.setConfiguration(null);
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
  }

  @Test
  void wrongVersionsAndUnrelatedPluginGoalsDoNotConferOwnership() {
    var module = module("worker");
    var owner = aggregate(module);
    var dependency = owner.getDependencies().getFirst();
    dependency.setVersion("old");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    dependency.setVersion("1");
    dependency.setGroupId("other");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    dependency.setGroupId("test");
    var stage = owner.getBuildPlugins().get(1);
    stage.getExecutions().getFirst().setGoals(List.of("copy-dependencies"));
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
    stage.getExecutions().getFirst().setGoals(List.of("unpack-dependencies"));
    stage.setArtifactId("unrelated-plugin");
    assertFalse(CoverageOwnership.owned(module, List.of(owner)));
  }

  private static MavenProject module(String name) {
    var project = new MavenProject();
    project.setGroupId("test");
    project.setArtifactId(name);
    project.setVersion("1");
    project.setBuild(new Build());
    return project;
  }

  private static MavenProject aggregate(MavenProject module) {
    var project = module("coverage");
    var dependency = new Dependency();
    dependency.setGroupId(module.getGroupId());
    dependency.setArtifactId(module.getArtifactId());
    dependency.setVersion(module.getVersion());
    project.getDependencies().add(dependency);
    var jacoco = new Plugin();
    jacoco.setGroupId("org.jacoco");
    jacoco.setArtifactId("jacoco-maven-plugin");
    var check = new PluginExecution();
    check.setGoals(new java.util.ArrayList<>(List.of("report-aggregate", "check")));
    jacoco.addExecution(check);
    project.getBuild().addPlugin(jacoco);
    var unpack = new Plugin();
    unpack.setArtifactId("maven-dependency-plugin");
    var execution = new PluginExecution();
    execution.addGoal("unpack-dependencies");
    var config = new Xpp3Dom("configuration");
    child(config, "includeArtifactIds", module.getArtifactId());
    execution.setConfiguration(config);
    unpack.addExecution(execution);
    project.getBuild().addPlugin(unpack);
    return project;
  }

  private static void child(Xpp3Dom config, String name, String value) {
    var child = new Xpp3Dom(name);
    child.setValue(value);
    config.addChild(child);
  }
}
