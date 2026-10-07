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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

class PolicyInspectorTest {
  private static Model platform() {
    Model model = new Model();
    model.getProperties().setProperty("future.library.version", "1.2.3");
    model.getProperties().setProperty("forwardmeasure.jpa.version", "1.1.0");
    Dependency dependency = new Dependency();
    dependency.setGroupId("example");
    dependency.setArtifactId("future-bom");
    dependency.setType("pom");
    dependency.setScope("import");
    dependency.setVersion("${future.library.version}");
    DependencyManagement dm = new DependencyManagement();
    dm.addDependency(dependency);
    model.setDependencyManagement(dm);
    Plugin plugin = new Plugin();
    plugin.setArtifactId("future-plugin");
    plugin.setVersion("${future.library.version}");
    PluginManagement pm = new PluginManagement();
    pm.addPlugin(plugin);
    Build build = new Build();
    build.setPluginManagement(pm);
    model.setBuild(build);
    return model;
  }

  private final PolicyInspector inspector = new PolicyInspector(PlatformPolicy.from(platform()));

  @Test
  void discoversNewPlatformOwnershipWithoutEditingJava() {
    Model consumer = new Model();
    consumer.getProperties().setProperty("future.library.version", "9");
    consumer.getProperties().setProperty("component.setting", "enabled");
    assertEquals(1, inspector.inspect(consumer).size());
  }

  @Test
  void inspectsInactiveProfilesAndDefaultPluginGroup() {
    Model consumer = new Model();
    Profile profile = new Profile();
    profile.setId("inactive");
    profile.getProperties().setProperty("future.library.version", "9");
    consumer.addProfile(profile);
    Plugin plugin = new Plugin();
    plugin.setArtifactId("future-plugin");
    plugin.setVersion("9");
    Build build = new Build();
    build.addPlugin(plugin);
    consumer.setBuild(build);
    assertEquals(2, inspector.inspect(consumer).size());
  }

  @Test
  void permitsBomOrderingUsingThePlatformVersionExpressionOnly() {
    Model consumer = new Model();
    Dependency dependency =
        platform().getDependencyManagement().getDependencies().getFirst().clone();
    dependency.setType("pom");
    dependency.setScope("import");
    DependencyManagement management = new DependencyManagement();
    management.addDependency(dependency);
    consumer.setDependencyManagement(management);
    assertTrue(inspector.inspect(consumer).isEmpty());
    dependency.setVersion("9");
    assertEquals(1, inspector.inspect(consumer).size());
  }

  @Test
  void acceptsOnlyDocumentedExactLocalOverrides() {
    Model consumer = new Model();
    consumer.getProperties().setProperty("future.library.version", "9");
    override(
        consumer, "property:future.library.version", "Framework compatibility requires version 9.");
    assertTrue(inspector.inspect(consumer).isEmpty());
    override(consumer, "property:future.library.version", "  ");
    assertEquals(2, inspector.inspect(consumer).size());
    override(consumer, "property:*", "Wildcard bypass");
    assertEquals(2, inspector.inspect(consumer).size());
  }

  @Test
  void neverAllowsFirstPartyVersionOverrides() {
    Model consumer = new Model();
    consumer.getProperties().setProperty("forwardmeasure.jpa.version", "9");
    override(consumer, "property:forwardmeasure.jpa.version", "Requested exception");
    assertEquals(2, inspector.inspect(consumer).size());
  }

  @Test
  void rejectsNewLocalLibraryPinsBeforeTheyEnterThePlatformCatalogue() {
    Model consumer = new Model();
    Dependency dependency = new Dependency();
    dependency.setGroupId("example");
    dependency.setArtifactId("new-library");
    dependency.setVersion("9");
    consumer.addDependency(dependency);
    assertEquals(1, inspector.inspect(consumer).size());
    dependency.setVersion("${future.library.version}");
    assertTrue(inspector.inspect(consumer).isEmpty());
  }

  @Test
  void distinguishesProductionAndClassifiedArtifacts() {
    Model authority = platform();
    Dependency production = new Dependency();
    production.setGroupId("example");
    production.setArtifactId("library");
    production.setVersion("${future.library.version}");
    authority.getDependencyManagement().addDependency(production);
    Model consumer = new Model();
    Dependency tests = production.clone();
    tests.setType("test-jar");
    tests.setClassifier("tests");
    consumer.addDependency(tests);
    assertTrue(new PolicyInspector(PlatformPolicy.from(authority)).inspect(consumer).isEmpty());
    tests.setType("jar");
    tests.setClassifier(null);
    assertEquals(1, new PolicyInspector(PlatformPolicy.from(authority)).inspect(consumer).size());
  }

  @Test
  void preservesExclusionsWithoutCreatingAnotherVersionPin() {
    Model consumer = new Model();
    DependencyManagement management = new DependencyManagement();
    Dependency dependency =
        platform().getDependencyManagement().getDependencies().getFirst().clone();
    org.apache.maven.model.Exclusion exclusion = new org.apache.maven.model.Exclusion();
    exclusion.setGroupId("optional");
    exclusion.setArtifactId("provider");
    dependency.addExclusion(exclusion);
    dependency.setScope(null);
    management.addDependency(dependency);
    consumer.setDependencyManagement(management);
    assertTrue(inspector.inspect(consumer).isEmpty());
    dependency.setVersion("9");
    assertEquals(1, inspector.inspect(consumer).size());
  }

  @Test
  void exceptionOnAnotherPomDoesNotAuthorizeTheChild() {
    Model parent = new Model();
    override(parent, "property:future.library.version", "Parent framework compatibility");
    Model child = new Model();
    child.getProperties().setProperty("future.library.version", "9");
    assertTrue(inspector.inspect(parent).isEmpty());
    assertEquals(1, inspector.inspect(child).size());
  }

  @Test
  void projectReleaseVersionCannotOverrideAnotherProductsDependencyVersion() {
    Model consumer = new Model();
    Dependency dependency = new Dependency();
    dependency.setGroupId("com.forwardmeasure.product");
    dependency.setArtifactId("model");
    dependency.setVersion("${project.version}");
    consumer.addDependency(dependency);
    assertTrue(
        new PolicyInspector(PlatformPolicy.from(platform()), "com.forwardmeasure.product")
            .inspect(consumer)
            .isEmpty());
    assertEquals(
        1,
        new PolicyInspector(PlatformPolicy.from(platform()), "com.forwardmeasure.other")
            .inspect(consumer)
            .size());
  }

  @Test
  void rejectsUnknownPropertyExpressionsAndCrossProductReleaseAliases() {
    for (String version :
        java.util.List.of(
            "${unknown.version}",
            "${revision}",
            "${project.version}",
            "${future.library.version")) {
      Model consumer = new Model();
      Dependency dependency = new Dependency();
      dependency.setGroupId("example");
      dependency.setArtifactId("new-library");
      dependency.setVersion(version);
      consumer.addDependency(dependency);
      assertEquals(1, inspector.inspect(consumer).size(), version);
    }
    Model consumer = new Model();
    consumer.getProperties().setProperty("revision", "2");
    consumer.getProperties().setProperty("forwardmeasure.unlisted.version", "2");
    consumer.getProperties().setProperty("forwardmeasure.setting", "enabled");
    assertEquals(1, inspector.inspect(consumer).size());
  }

  @Test
  void acceptsSameProductRevisionAndVersionlessDependencies() {
    Model consumer = new Model();
    Dependency dependency = new Dependency();
    dependency.setGroupId("${project.groupId}");
    dependency.setArtifactId("model");
    dependency.setVersion("${revision}");
    consumer.addDependency(dependency);
    assertTrue(inspector.inspect(consumer).isEmpty());
    dependency.setVersion(null);
    assertTrue(inspector.inspect(consumer).isEmpty());
  }

  @Test
  void managedLibraryRedeclarationsRequireScopeOrExclusions() {
    Model authority = platform();
    Dependency dependency = new Dependency();
    dependency.setGroupId("example");
    dependency.setArtifactId("library");
    dependency.setVersion("${future.library.version}");
    authority.getDependencyManagement().addDependency(dependency.clone());
    var local = new PolicyInspector(PlatformPolicy.from(authority));
    Model consumer = new Model();
    DependencyManagement management = new DependencyManagement();
    management.addDependency(dependency);
    consumer.setDependencyManagement(management);
    assertEquals(1, local.inspect(consumer).size());
    dependency.setScope("runtime");
    assertTrue(local.inspect(consumer).isEmpty());
  }

  @Test
  void inspectsPluginManagementAndRequiresPlatformPropertiesForNewPlugins() {
    Model consumer = new Model();
    Build build = new Build();
    consumer.setBuild(build);
    PluginManagement management = new PluginManagement();
    build.setPluginManagement(management);
    Plugin plugin = new Plugin();
    plugin.setArtifactId("new-plugin");
    management.addPlugin(plugin);
    assertTrue(inspector.inspect(consumer).isEmpty());
    plugin.setVersion("9");
    assertEquals(1, inspector.inspect(consumer).size());
    plugin.setVersion("${future.library.version}");
    assertTrue(inspector.inspect(consumer).isEmpty());
  }

  @Test
  void rejectsFirstPartyDependencyExceptionsAndDuplicateExceptions() {
    Model consumer = new Model();
    override(consumer, "dependency:com.forwardmeasure.product:model", "compatibility");
    assertTrue(inspector.inspect(consumer).getFirst().contains("cannot be overridden"));
    override(consumer, "property:future.library.version", "compatibility");
    Xpp3Dom config = (Xpp3Dom) consumer.getBuild().getPlugins().getFirst().getConfiguration();
    Xpp3Dom overrides = config.getChild("overrides");
    overrides.addChild(new Xpp3Dom(overrides.getChild("override")));
    assertTrue(inspector.inspect(consumer).getFirst().contains("duplicate override"));
  }

  @Test
  void rejectsMissingOverrideKeysAndReasons() {
    for (String missing : java.util.List.of("key", "reason")) {
      Model consumer = new Model();
      override(consumer, "property:future.library.version", "compatibility");
      Xpp3Dom config = (Xpp3Dom) consumer.getBuild().getPlugins().getFirst().getConfiguration();
      Xpp3Dom entry = config.getChild("overrides").getChild("override");
      entry.getChild(missing).setValue(null);
      assertEquals(1, inspector.inspect(consumer).size());
      entry.removeChild(entry.getChild(missing));
      assertEquals(1, inspector.inspect(consumer).size());
    }
  }

  @Test
  void profileExceptionsApplyOnlyWithinThatProfile() {
    Model consumer = new Model();
    consumer.getProperties().setProperty("future.library.version", "9");
    Model exceptionHolder = new Model();
    override(exceptionHolder, "property:future.library.version", "profile compatibility");
    Profile profile = new Profile();
    profile.setId("framework");
    profile.setBuild(exceptionHolder.getBuild());
    profile.getProperties().setProperty("future.library.version", "9");
    consumer.addProfile(profile);
    assertEquals(1, inspector.inspect(consumer).size());
  }

  @Test
  void unrelatedPluginConfigurationDoesNotAuthorizeAnException() {
    Model consumer = new Model();
    override(consumer, "property:future.library.version", "compatibility");
    var plugin = consumer.getBuild().getPlugins().getFirst();
    consumer.getProperties().setProperty("future.library.version", "9");
    plugin.setArtifactId("unrelated-plugin");
    assertEquals(1, inspector.inspect(consumer).size());
    plugin.setArtifactId("forwardmeasure-platform-policy-maven-plugin");
    plugin.setConfiguration(new Xpp3Dom("configuration"));
    assertEquals(1, inspector.inspect(consumer).size());
    plugin.setConfiguration(null);
    assertEquals(1, inspector.inspect(consumer).size());
  }

  private static void override(Model model, String key, String reason) {
    Xpp3Dom config = new Xpp3Dom("configuration");
    Xpp3Dom overrides = new Xpp3Dom("overrides");
    Xpp3Dom override = new Xpp3Dom("override");
    Xpp3Dom keyNode = new Xpp3Dom("key");
    keyNode.setValue(key);
    Xpp3Dom reasonNode = new Xpp3Dom("reason");
    reasonNode.setValue(reason);
    override.addChild(keyNode);
    override.addChild(reasonNode);
    overrides.addChild(override);
    config.addChild(overrides);
    Plugin plugin = new Plugin();
    plugin.setGroupId("com.forwardmeasure.platform");
    plugin.setArtifactId("forwardmeasure-platform-policy-maven-plugin");
    plugin.setConfiguration(config);
    Build build = new Build();
    build.addPlugin(plugin);
    model.setBuild(build);
  }
}
