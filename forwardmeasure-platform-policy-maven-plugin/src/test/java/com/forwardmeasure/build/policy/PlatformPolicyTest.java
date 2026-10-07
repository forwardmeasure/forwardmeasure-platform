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
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.junit.jupiter.api.Test;

class PlatformPolicyTest {
  @Test
  void handlesEmptyAuthorityAndDirectPluginVersions() {
    var model = new Model();
    var empty = PlatformPolicy.from(model);
    assertTrue(empty.properties().isEmpty());
    assertTrue(empty.dependencies().isEmpty());
    assertTrue(empty.plugins().isEmpty());
    var build = new Build();
    model.setBuild(build);
    var versioned = new Plugin();
    versioned.setArtifactId("maven-example-plugin");
    versioned.setVersion("1");
    build.addPlugin(versioned);
    var inherited = new Plugin();
    inherited.setArtifactId("maven-inherited-plugin");
    build.addPlugin(inherited);
    assertEquals(
        java.util.Map.of("org.apache.maven.plugins:maven-example-plugin", "1"),
        PlatformPolicy.from(model).plugins());
  }

  @Test
  void normalizesOnlyUnclassifiedJarCoordinates() {
    var dependency = new Dependency();
    dependency.setGroupId("example");
    dependency.setArtifactId("library");
    assertEquals("example:library", PlatformPolicy.dependencyKey(dependency));
    dependency.setClassifier("");
    assertEquals("example:library", PlatformPolicy.dependencyKey(dependency));
    dependency.setType(null);
    assertEquals("example:library", PlatformPolicy.dependencyKey(dependency));
    dependency.setType("jar");
    dependency.setClassifier("tests");
    assertEquals("example:library:jar:tests", PlatformPolicy.dependencyKey(dependency));
    dependency.setType("pom");
    dependency.setClassifier(null);
    assertEquals("example:library:pom:", PlatformPolicy.dependencyKey(dependency));
  }
}
