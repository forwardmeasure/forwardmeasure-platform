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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckHeadersMojoTest {
  private static final String ASF =
      "#!/usr/bin/env bash\n"
          + "# Licensed to the Apache Software Foundation (ASF) under one or more\n"
          + "# contributor license agreements.\n"
          + "echo ok\n";

  @TempDir Path module;

  @Test
  void aScriptWithoutAHeaderFailsTheBuildAndIsNamed() throws Exception {
    write("scripts/build.sh", "#!/usr/bin/env bash\necho no header\n");
    write("scripts/release.sh", ASF);
    var failure = assertThrows(MojoFailureException.class, mojo()::execute);
    assertTrue(failure.getMessage().contains("scripts/build.sh"), failure.getMessage());
    assertFalse(failure.getMessage().contains("release.sh"), failure.getMessage());
    assertTrue(failure.getMessage().startsWith("1 file(s)"), failure.getMessage());
  }

  @Test
  void everyApprovedHeaderFormPasses() {
    write("a.sh", ASF);
    write("b.cmd", "@REM Licensed under the Apache License, Version 2.0 (the \"License\");\n");
    write(
        "src/main/docker/Dockerfile.jvm",
        "# Copyright (c) 2026 ForwardMeasure, Inc. All rights reserved.\nFROM scratch\n");
    assertDoesNotThrow(mojo()::execute);
  }

  @Test
  void eachDefaultFileTypeIsChecked() {
    for (String name :
        new String[] {
          "x.sh",
          "x.bash",
          "x.cmd",
          "x.bat",
          "x.ps1",
          "deploy/x.yaml.gotmpl",
          "templates/_x.tpl",
          "Dockerfile",
          ".github/workflows/build.yml",
          ".github/dependabot.yaml"
        }) {
      write(name, "no header\n");
    }
    var failure = assertThrows(MojoFailureException.class, mojo()::execute);
    assertTrue(failure.getMessage().startsWith("10 file(s)"), failure.getMessage());
  }

  @Test
  void aHeaderBelowTheConfiguredLineLimitDoesNotCount() {
    write(
        "late.sh",
        "#!/bin/sh\n"
            + "echo filler\n".repeat(45)
            + "# Licensed to the Apache Software Foundation (ASF)\n");
    assertThrows(MojoFailureException.class, mojo()::execute);
    var lenient = mojo();
    lenient.headerLines = 60;
    assertDoesNotThrow(lenient::execute);
  }

  @Test
  void childModulesBuildOutputAndOtherFileTypesAreLeftAlone() {
    write("child/pom.xml", "<project/>");
    write("child/run.sh", "no header: the child module checks its own files\n");
    write("target/generated.sh", "no header: build output\n");
    write("nested/target/generated.sh", "no header: build output\n");
    write("README.md", "no header: Markdown is not a checked type\n");
    write("src/main/java/Example.java", "no header: RAT checks Java\n");
    assertDoesNotThrow(mojo()::execute);
  }

  @Test
  void excludedFilesAndCustomApprovedHeadersAreHonoured() {
    write("vendor/third-party.sh", "# MIT licensed\n");
    var mojo = mojo();
    mojo.excludes = java.util.List.of("vendor/**");
    assertDoesNotThrow(mojo::execute);
    mojo.excludes = CheckHeadersMojo.DEFAULT_EXCLUDES;
    assertThrows(MojoFailureException.class, mojo::execute);
    mojo.approvedHeaders = java.util.List.of("MIT licensed");
    assertDoesNotThrow(mojo::execute);
  }

  @Test
  void aModuleWithoutCheckedFilesOrWithoutADirectoryPasses() throws Exception {
    assertDoesNotThrow(mojo()::execute);
    var absent = mojo();
    absent.basedir = module.resolve("does-not-exist").toFile();
    assertDoesNotThrow(absent::execute);
    assertEquals(0, Files.list(module).count());
  }

  private CheckHeadersMojo mojo() {
    var mojo = new CheckHeadersMojo();
    mojo.basedir = module.toFile();
    return mojo;
  }

  private void write(String relative, String content) {
    try {
      Path file = module.resolve(relative);
      Files.createDirectories(file.getParent());
      Files.writeString(file, content);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }
}
