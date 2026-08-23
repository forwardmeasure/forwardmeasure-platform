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

import java.util.Properties;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.junit.jupiter.api.Test;

class PolicyInspectorTest {
  private final PolicyInspector inspector = new PolicyInspector();

  @Test
  void acceptsComponentOwnedVersions() {
    Model model = new Model();
    Properties properties = new Properties();
    properties.setProperty("pekko.version", "1.2.0");
    model.setProperties(properties);
    assertTrue(inspector.inspect(model).isEmpty());
  }

  @Test
  void rejectsKeycloakVersionProperty() {
    Model model = new Model();
    Properties properties = new Properties();
    properties.setProperty("keycloak.version", "0.0.0");
    model.setProperties(properties);
    assertEquals(1, inspector.inspect(model).size());
  }

  @Test
  void rejectsPlatformOwnedPropertyDependencyAndPluginVersions() {
    Model model = new Model();
    Properties properties = new Properties();
    properties.setProperty("jackson.version", "0.0.0");
    model.setProperties(properties);
    Dependency dependency = new Dependency();
    dependency.setGroupId("org.postgresql");
    dependency.setArtifactId("postgresql");
    dependency.setVersion("0.0.0");
    model.addDependency(dependency);
    Plugin plugin = new Plugin();
    plugin.setGroupId("org.apache.maven.plugins");
    plugin.setArtifactId("maven-compiler-plugin");
    plugin.setVersion("0.0.0");
    Build build = new Build();
    build.addPlugin(plugin);
    model.setBuild(build);
    assertEquals(3, inspector.inspect(model).size());
  }
}
