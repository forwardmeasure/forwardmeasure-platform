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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Class-level provenance keeps generated exemptions from hiding handwritten code in mixed modules.
 */
final class GeneratedCoverage {
  static final String EXCLUDES_PROPERTY = "forwardmeasure.coverage.generated.excludes";
  private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");

  record Inventory(Set<String> generated, boolean handwritten) {}

  static Inventory inspect(Path classes, List<MavenProject> projects) throws IOException {
    Set<String> generatedSources = new HashSet<>();
    Set<String> handwrittenSources = new HashSet<>();
    for (var project : projects) {
      Path build = Path.of(project.getBuild().getDirectory()).toAbsolutePath().normalize();
      index(build.resolve("generated-sources"), generatedSources);
      for (String sourceRoot : project.getCompileSourceRoots()) {
        Path root = Path.of(sourceRoot).toAbsolutePath().normalize();
        if (!root.startsWith(build)) index(root, handwrittenSources);
        else index(root, generatedSources);
      }
    }
    Set<String> excluded = new TreeSet<>();
    boolean handwritten = false;
    if (Files.isDirectory(classes)) {
      try (var paths = Files.walk(classes)) {
        for (Path path :
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".class"))
                .toList()) {
          if (path.getFileName().toString().equals("module-info.class")) continue;
          var metadata = new Metadata();
          new ClassReader(Files.readAllBytes(path))
              .accept(metadata, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
          String source = metadata.sourceKey();
          boolean generated =
              metadata.generatedAnnotation
                  || (generatedSources.contains(source) && !handwrittenSources.contains(source));
          if (generated) excluded.add(classes.relativize(path).toString().replace('\\', '/'));
          else handwritten = true;
        }
      }
    }
    return new Inventory(excluded, handwritten);
  }

  private static void index(Path root, Set<String> sources) throws IOException {
    if (!Files.isDirectory(root)) return;
    try (var paths = Files.walk(root)) {
      for (Path source :
          paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
        var matcher = PACKAGE.matcher(Files.readString(source));
        String prefix = matcher.find() ? matcher.group(1).replace('.', '/') + "/" : "";
        sources.add(prefix + source.getFileName());
      }
    }
  }

  private static final class Metadata extends ClassVisitor {
    private String name;
    private String source;
    private boolean generatedAnnotation;

    Metadata() {
      super(Opcodes.ASM9);
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String parent,
        String[] interfaces) {
      this.name = name;
    }

    @Override
    public void visitSource(String source, String debug) {
      this.source = source;
    }

    @Override
    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (descriptor.endsWith("/Generated;") || descriptor.endsWith("$Generated;"))
        generatedAnnotation = true;
      return null;
    }

    String sourceKey() {
      int slash = name.lastIndexOf('/');
      return source == null ? "" : name.substring(0, slash + 1) + source;
    }
  }
}
