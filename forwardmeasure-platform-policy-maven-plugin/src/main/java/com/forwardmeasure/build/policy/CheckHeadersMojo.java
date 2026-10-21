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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Requires a license header in the files Apache RAT never examines.
 *
 * <p>RAT asks Apache Tika for each file's media type and treats anything that is neither {@code
 * text/*} nor on its short built-in list as binary, which it does not check: shell scripts ({@code
 * application/x-sh}), Windows scripts and some template formats among them. It also skips hidden
 * directories such as {@code .github}. This goal checks those files instead: each must carry one of
 * the approved header texts within its first lines.
 *
 * <p>Each module checks the files under its own directory; directories of child modules (those
 * holding a {@code pom.xml}) are left to the child.
 */
@Mojo(name = "check-headers", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class CheckHeadersMojo extends AbstractMojo {

  static final List<String> DEFAULT_INCLUDES =
      List.of(
          "**.sh",
          "**.bash",
          "**.cmd",
          "**.bat",
          "**.ps1",
          "**.gotmpl",
          "**.tpl",
          "**Dockerfile*",
          ".github/**.yml",
          ".github/**.yaml");

  static final List<String> DEFAULT_EXCLUDES =
      List.of("target/**", "**/target/**", ".git/**", "**/node_modules/**");

  /** The ASF header, its short form, and ForwardMeasure's proprietary header. */
  static final List<String> DEFAULT_APPROVED_HEADERS =
      List.of(
          "Licensed to the Apache Software Foundation \\(ASF\\)",
          "Licensed under the Apache License, Version 2\\.0",
          "Copyright \\(c\\) \\d{4} ForwardMeasure, Inc\\. All rights reserved\\.");

  @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
  File basedir;

  /** Glob patterns, relative to the module directory, of the files to check. */
  @Parameter List<String> includes = DEFAULT_INCLUDES;

  /** Glob patterns, relative to the module directory, of files never to check. */
  @Parameter List<String> excludes = DEFAULT_EXCLUDES;

  /** Regular expressions; a file passes when one of them matches within its first lines. */
  @Parameter List<String> approvedHeaders = DEFAULT_APPROVED_HEADERS;

  /** How many lines from the top of a file may come before the header (shebangs, front matter). */
  @Parameter(defaultValue = "40")
  int headerLines = 40;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!basedir.isDirectory()) return;
    Path root = basedir.toPath();
    List<PathMatcher> included = matchers(includes);
    List<PathMatcher> excluded = matchers(excludes);
    List<Pattern> approved = approvedHeaders.stream().map(Pattern::compile).toList();
    List<String> missing = new ArrayList<>();
    int checked = 0;
    try {
      List<Path> candidates = candidates(root, included, excluded);
      for (Path file : candidates) {
        checked++;
        if (!hasApprovedHeader(file, approved, headerLines))
          missing.add(root.relativize(file).toString());
      }
    } catch (IOException | UncheckedIOException e) {
      throw new MojoExecutionException("Could not read files under " + root, e);
    }
    if (!missing.isEmpty()) {
      throw new MojoFailureException(
          missing.size()
              + " file(s) without an approved license header (Apache RAT does not check these"
              + " file types):\n  "
              + String.join("\n  ", missing));
    }
    getLog().info("License headers present in " + checked + " file(s) RAT does not check");
  }

  private static List<PathMatcher> matchers(List<String> globs) {
    return globs.stream()
        .map(glob -> FileSystems.getDefault().getPathMatcher("glob:" + glob))
        .toList();
  }

  private static List<Path> candidates(
      Path root, List<PathMatcher> included, List<PathMatcher> excluded) throws IOException {
    List<Path> files = new ArrayList<>();
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
            if (dir.equals(root)) return FileVisitResult.CONTINUE;
            Path relative = root.relativize(dir);
            if (matchesAny(excluded, relative) || matchesAny(excluded, relative.resolve("x")))
              return FileVisitResult.SKIP_SUBTREE;
            // A directory with its own POM is a module that checks its own files.
            if (Files.isRegularFile(dir.resolve("pom.xml"))) return FileVisitResult.SKIP_SUBTREE;
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            Path relative = root.relativize(file);
            if (attributes.isRegularFile()
                && matchesAny(included, relative)
                && !matchesAny(excluded, relative)) files.add(file);
            return FileVisitResult.CONTINUE;
          }
        });
    files.sort(null);
    return files;
  }

  private static boolean matchesAny(List<PathMatcher> matchers, Path path) {
    for (PathMatcher matcher : matchers) if (matcher.matches(path)) return true;
    return false;
  }

  static boolean hasApprovedHeader(Path file, List<Pattern> approved, int headerLines)
      throws IOException {
    var decoder =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    try (BufferedReader reader =
        new BufferedReader(new java.io.InputStreamReader(Files.newInputStream(file), decoder))) {
      StringBuilder head = new StringBuilder();
      String line;
      for (int i = 0; i < headerLines && (line = reader.readLine()) != null; i++) {
        head.append(line).append('\n');
      }
      for (Pattern pattern : approved) if (pattern.matcher(head).find()) return true;
      return false;
    }
  }
}
