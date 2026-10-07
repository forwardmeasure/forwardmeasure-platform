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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class GeneratedCoverageTest {
  @TempDir Path directory;

  @Test
  void mixedPackagesExcludeOnlyGeneratedSourcesIncludingInnerAndSecondaryClasses()
      throws Exception {
    var project = project("mixed");
    source(project, true, "same/Dto.java", "package same; public class Dto {} class Helper {}\n");
    source(project, false, "same/Service.java", "package same; public class Service {}\n");
    Path classes = classes(project);
    emit(classes, "same/Dto", "Dto.java", null);
    emit(classes, "same/Dto$Inner", "Dto.java", null);
    emit(classes, "same/Helper", "Dto.java", null);
    emit(classes, "same/Service", "Service.java", null);
    var result = GeneratedCoverage.inspect(classes, List.of(project));
    assertTrue(result.handwritten());
    assertEquals(
        Set.of("same/Dto.class", "same/Dto$Inner.class", "same/Helper.class"), result.generated());
  }

  @Test
  void duplicateSimpleNamesAndStaleGeneratedCopiesCannotExemptHandwrittenClasses()
      throws Exception {
    var project = project("collision");
    source(project, true, "generated/Dto.java", "package generated; public class Dto {}\n");
    source(project, true, "same/Service.java", "package same; public class Service {}\n");
    source(project, false, "same/Service.java", "package same; public class Service {}\n");
    Path classes = classes(project);
    emit(classes, "owned/Dto", "Dto.java", null);
    emit(classes, "same/Service", "Service.java", null);
    var result = GeneratedCoverage.inspect(classes, List.of(project));
    assertTrue(result.handwritten());
    assertTrue(result.generated().isEmpty());
  }

  @Test
  void retainedGeneratedMarkersCoverFrameworkOutputWithoutSourceFiles() throws Exception {
    var project = project("annotations");
    Path classes = classes(project);
    emit(classes, "framework/Bean", null, "Lio/micronaut/core/annotation/Generated;");
    emit(classes, "framework/Other", null, "Lfixture/Markers$Generated;");
    emit(classes, "framework/Ordinary", null, "Lfixture/NotGenerated;");
    var result = GeneratedCoverage.inspect(classes, List.of(project));
    assertEquals(Set.of("framework/Bean.class", "framework/Other.class"), result.generated());
    assertTrue(result.handwritten());
  }

  @Test
  void absentSourceMetadataRemainsSubjectToCoverageAndDescriptorsDoNot() throws Exception {
    var project = project("unknown");
    Path classes = classes(project);
    Files.write(classes.resolve("module-info.class"), new byte[] {1});
    Files.writeString(classes.resolve("resource.txt"), "not a class");
    assertFalse(GeneratedCoverage.inspect(classes, List.of(project)).handwritten());
    emit(classes, "Unknown", null, null);
    assertTrue(GeneratedCoverage.inspect(classes, List.of(project)).handwritten());
    assertTrue(
        GeneratedCoverage.inspect(classes.resolve("missing"), List.of(project))
            .generated()
            .isEmpty());
  }

  @Test
  void customBuildOutputRootsAndDefaultPackageSourcesAreRecognized() throws Exception {
    var project = project("custom");
    Path custom = Path.of(project.getBuild().getDirectory()).resolve("codegen/java");
    Files.createDirectories(custom);
    Files.writeString(custom.resolve("Plain.java"), "public class Plain {}\n");
    project.addCompileSourceRoot(custom.toString());
    Path classes = classes(project);
    emit(classes, "Plain", "Plain.java", null);
    var result = GeneratedCoverage.inspect(classes, List.of(project));
    assertEquals(Set.of("Plain.class"), result.generated());
    assertFalse(result.handwritten());
  }

  @Test
  void aggregateStagingRetainsUpstreamGeneratedProvenance() throws Exception {
    var upstream = project("upstream");
    source(upstream, true, "api/Client.java", "package api; public class Client {}\n");
    var aggregate = project("aggregate");
    Path staged = classes(aggregate);
    emit(staged, "api/Client", "Client.java", null);
    emit(staged, "owned/Service", "Service.java", null);
    var result = GeneratedCoverage.inspect(staged, List.of(upstream, aggregate));
    assertEquals(Set.of("api/Client.class"), result.generated());
    assertTrue(result.handwritten());
  }

  @Test
  void generatedOnlyModulesNeedNoExecutionDataButAddingHandwrittenCodeRequiresIt()
      throws Exception {
    var project = project("gate");
    source(project, true, "api/Client.java", "package api; public class Client {}\n");
    Path classes = classes(project);
    emit(classes, "api/Client", "Client.java", null);
    var mojo = mojo(project, classes);
    mojo.execute();
    assertEquals(
        "api/Client.class",
        project.getProperties().getProperty(GeneratedCoverage.EXCLUDES_PROPERTY));
    assertEquals(
        List.of("api/Client.class"),
        Files.readAllLines(classes.getParent().resolve("coverage-generated-excludes.txt")));
    emit(classes, "owned/Service", "Service.java", null);
    assertThrows(MojoFailureException.class, mojo::execute);
    Files.write(mojo.dataFile.toPath(), new byte[] {1});
    mojo.execute();
    mojo.aggregateOwnsCoverage = true;
    Files.delete(mojo.dataFile.toPath());
    assertThrows(MojoFailureException.class, mojo::execute);
  }

  @Test
  void malformedBytecodeCannotSilentlyBecomeACoverageExemption() throws Exception {
    var project = project("invalid");
    Path classes = classes(project);
    Files.write(classes.resolve("Broken.class"), new byte[] {1});
    assertThrows(MojoExecutionException.class, () -> mojo(project, classes).execute());
  }

  private RequireCoverageDataMojo mojo(MavenProject project, Path classes) {
    var mojo = new RequireCoverageDataMojo();
    mojo.project = project;
    mojo.reactorProjects = List.of(project);
    mojo.classesDirectory = classes.toFile();
    mojo.dataFile = classes.getParent().resolve("jacoco.exec").toFile();
    return mojo;
  }

  private MavenProject project(String name) throws Exception {
    var project = new MavenProject();
    Path basedir = directory.resolve(name);
    Files.createDirectories(basedir);
    var build = new Build();
    build.setDirectory(basedir.resolve("target").toString());
    project.setBuild(build);
    project.addCompileSourceRoot(basedir.resolve("src/main/java").toString());
    return project;
  }

  private static Path classes(MavenProject project) throws Exception {
    return Files.createDirectories(Path.of(project.getBuild().getDirectory()).resolve("classes"));
  }

  private static void source(MavenProject project, boolean generated, String file, String text)
      throws Exception {
    Path path =
        generated
            ? Path.of(project.getBuild().getDirectory())
                .resolve("generated-sources/openapi/src/main/java")
                .resolve(file)
            : Path.of(project.getCompileSourceRoots().getFirst()).resolve(file);
    Files.createDirectories(path.getParent());
    Files.writeString(path, text);
  }

  private static void emit(Path root, String name, String source, String annotation)
      throws Exception {
    var writer = new ClassWriter(0);
    writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
    if (source != null) writer.visitSource(source, null);
    if (annotation != null) writer.visitAnnotation(annotation, false).visitEnd();
    writer.visitEnd();
    Path path = root.resolve(name + ".class");
    Files.createDirectories(path.getParent());
    Files.write(path, writer.toByteArray());
  }
}
