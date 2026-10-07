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

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RequireCoverageDataMojoTest {
  @TempDir Path directory;

  @Test
  void missingAndEmptyExecutionDataCannotPassForProductionClasses() throws Exception {
    var mojo = module();
    Files.write(mojo.classesDirectory.toPath().resolve("Service.class"), new byte[] {1});
    assertThrows(MojoFailureException.class, mojo::execute);
    Files.createFile(mojo.dataFile.toPath());
    assertThrows(MojoFailureException.class, mojo::execute);
  }

  @Test
  void resourcesOnlyModulesDoNotRequireJavaCoverage() throws Exception {
    var mojo = module();
    Files.writeString(mojo.classesDirectory.toPath().resolve("application.yaml"), "enabled: true");
    assertDoesNotThrow(mojo::execute);
  }

  @Test
  void aggregateOwnershipAndExplicitTestSkippingDoNotRequireLocalData() throws Exception {
    var mojo = module();
    Files.write(mojo.classesDirectory.toPath().resolve("Service.class"), new byte[] {1});
    mojo.aggregateOwnsCoverage = true;
    assertDoesNotThrow(mojo::execute);
    mojo.aggregateOwnsCoverage = false;
    mojo.skipTests = true;
    assertDoesNotThrow(mojo::execute);
    mojo.skipTests = false;
    mojo.skipAllTests = true;
    assertDoesNotThrow(mojo::execute);
  }

  @Test
  void absentClassesAndModuleDescriptorsDoNotRequireExecutionData() throws Exception {
    var mojo = module();
    Files.delete(mojo.classesDirectory.toPath());
    assertDoesNotThrow(mojo::execute);
    Files.createDirectories(mojo.classesDirectory.toPath());
    Files.write(mojo.classesDirectory.toPath().resolve("module-info.class"), new byte[] {1});
    assertDoesNotThrow(mojo::execute);
  }

  @Test
  void permitsExecutionDataForTheSubsequentJacocoContentAndRatioCheck() throws Exception {
    var mojo = module();
    Files.write(mojo.classesDirectory.toPath().resolve("Service.class"), new byte[] {1});
    Files.write(mojo.dataFile.toPath(), new byte[] {1});
    assertDoesNotThrow(mojo::execute);
  }

  private RequireCoverageDataMojo module() throws Exception {
    var mojo = new RequireCoverageDataMojo();
    mojo.classesDirectory = Files.createDirectory(directory.resolve("classes")).toFile();
    mojo.dataFile = directory.resolve("jacoco.exec").toFile();
    return mojo;
  }
}
