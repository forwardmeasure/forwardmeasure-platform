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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

class CheckPolicyMojoTest {
  @Test
  void authorityMayDeclareItsOwnVersions() {
    var mojo = new CheckPolicyMojo();
    mojo.project = platform();
    assertDoesNotThrow(mojo::execute);
  }

  @Test
  void discoversAuthorityThroughIntermediateParents() {
    var authority = platform();
    var intermediate = project("com.forwardmeasure.platform", "framework-parent");
    intermediate.setParent(authority);
    var mojo = new CheckPolicyMojo();
    mojo.project = project("com.forwardmeasure.product", "service");
    mojo.project.setParent(intermediate);
    assertDoesNotThrow(mojo::execute);
  }

  @Test
  void unrelatedAncestorCannotMasqueradeAsThePlatform() {
    var mojo = new CheckPolicyMojo();
    mojo.project = project("unrelated", "forwardmeasure-platform");
    assertTrue(
        assertThrows(MojoFailureException.class, mojo::execute)
            .getMessage()
            .contains("ancestor is missing"));
  }

  @Test
  void inspectsOriginalConsumerDeclarationsAndReportsTheirCoordinates() {
    var mojo = new CheckPolicyMojo();
    mojo.project = project("com.forwardmeasure.product", "service");
    mojo.project.setParent(platform());
    mojo.project.getOriginalModel().getProperties().setProperty("library.version", "99");
    // An effective/inherited configuration cannot authorize an exception absent from this POM.
    var inherited = new CheckPolicyMojo.VersionOverride();
    inherited.key = "property:library.version";
    inherited.reason = "An ancestor's compatibility exception";
    mojo.overrides = List.of(inherited);
    String message = assertThrows(MojoFailureException.class, mojo::execute).getMessage();
    assertTrue(message.contains("com.forwardmeasure.product:service"));
    assertTrue(message.contains("property:library.version"));
  }

  private static MavenProject platform() {
    var project = project("com.forwardmeasure.platform", "forwardmeasure-platform");
    project.getOriginalModel().getProperties().setProperty("library.version", "1");
    return project;
  }

  private static MavenProject project(String group, String artifact) {
    Model model = new Model();
    model.setGroupId(group);
    model.setArtifactId(artifact);
    model.setVersion("1");
    var project = new MavenProject(model);
    project.setOriginalModel(model);
    return project;
  }
}
