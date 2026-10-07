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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/** Missing execution data must not turn an enabled coverage gate into a silent pass. */
@Mojo(name = "require-coverage-data", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class RequireCoverageDataMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  File classesDirectory;

  @Parameter(defaultValue = "${project.build.directory}/jacoco.exec", required = true)
  File dataFile;

  @Parameter(defaultValue = "${jacoco.module.check.skip}")
  boolean aggregateOwnsCoverage;

  @Parameter(property = "skipTests", defaultValue = "false")
  boolean skipTests;

  @Parameter(property = "maven.test.skip", defaultValue = "false")
  boolean skipAllTests;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
  List<MavenProject> reactorProjects;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    boolean skip = skipTests || skipAllTests || !classesDirectory.isDirectory();
    if (project != null) {
      project.getProperties().setProperty(CoverageOwnership.SKIP_PROPERTY, Boolean.toString(skip));
      if (!skip) {
        try {
          var inventory = GeneratedCoverage.inspect(classesDirectory.toPath(), reactorProjects);
          var generated = new java.util.TreeSet<>(inventory.generated());
          if (CoverageOwnership.isAggregate(project)) {
            for (var upstream : reactorProjects) {
              if (project.getDependencies().stream()
                  .noneMatch(
                      dependency ->
                          upstream.getGroupId().equals(dependency.getGroupId())
                              && upstream.getArtifactId().equals(dependency.getArtifactId())))
                continue;
              var manifest =
                  java.nio.file.Path.of(upstream.getBuild().getDirectory())
                      .resolve("coverage-generated-excludes.txt");
              if (Files.isRegularFile(manifest)) generated.addAll(Files.readAllLines(manifest));
            }
          }
          project
              .getProperties()
              .setProperty(GeneratedCoverage.EXCLUDES_PROPERTY, String.join(",", generated));
          Files.write(
              classesDirectory.toPath().getParent().resolve("coverage-generated-excludes.txt"),
              generated);
          boolean owned =
              aggregateOwnsCoverage && CoverageOwnership.owned(project, reactorProjects);
          skip = !inventory.handwritten() || owned;
          project
              .getProperties()
              .setProperty(CoverageOwnership.SKIP_PROPERTY, Boolean.toString(skip));
          getLog()
              .info(
                  "Coverage excludes "
                      + generated.size()
                      + " generated classes; handwritten classes present: "
                      + inventory.handwritten()
                      + "; aggregate owns module: "
                      + owned);
        } catch (IOException | RuntimeException failure) {
          throw new MojoExecutionException(
              "Cannot determine generated-code coverage provenance", failure);
        }
      }
    } else {
      skip |= aggregateOwnsCoverage;
    }
    if (skip) return;
    try (var files = Files.walk(classesDirectory.toPath())) {
      boolean hasClasses =
          files.anyMatch(
              path ->
                  Files.isRegularFile(path)
                      && path.toString().endsWith(".class")
                      && !path.getFileName().toString().equals("module-info.class"));
      if (hasClasses && (!dataFile.isFile() || dataFile.length() == 0)) {
        throw new MojoFailureException(
            "Production classes exist but coverage execution data is missing or empty: "
                + dataFile
                + ". Run instrumented tests; missing data is not a coverage pass.");
      }
    } catch (IOException failure) {
      throw new MojoExecutionException("Cannot inspect production classes for coverage", failure);
    }
  }
}
