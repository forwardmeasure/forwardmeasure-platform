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

import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/** Rejects local declarations that override the inherited ForwardMeasure platform. */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public final class CheckPolicyMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  /** Explicit local exceptions, also checked against the original POM by PolicyInspector. */
  @Parameter List<VersionOverride> overrides;

  public static final class VersionOverride {
    public String key;
    public String reason;
  }

  @Override
  public void execute() throws MojoFailureException {
    MavenProject platform = project;
    while (platform != null
        && !("com.forwardmeasure.platform".equals(platform.getGroupId())
            && "forwardmeasure-platform".equals(platform.getArtifactId())))
      platform = platform.getParent();
    if (platform == null)
      throw new MojoFailureException("ForwardMeasure platform ancestor is missing");
    // The authority POM itself necessarily declares all owned versions.
    if (platform == project) return;
    List<String> violations =
        new PolicyInspector(PlatformPolicy.from(platform.getOriginalModel()), project.getGroupId())
            .inspect(project.getOriginalModel());
    if (!violations.isEmpty()) {
      throw new MojoFailureException(
          "ForwardMeasure Java platform policy violations in "
              + project.getGroupId()
              + ":"
              + project.getArtifactId()
              + ":\n - "
              + String.join("\n - ", violations));
    }
    getLog().debug("ForwardMeasure Java platform policy passed");
  }
}
