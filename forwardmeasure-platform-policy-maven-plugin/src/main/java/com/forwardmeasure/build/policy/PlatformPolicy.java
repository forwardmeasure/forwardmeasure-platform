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

import java.util.Set;

final class PlatformPolicy {
  static final Set<String> MANAGED_PROPERTIES =
      Set.of(
          "maven.compiler.release",
          "jackson.version",
          "slf4j.version",
          "junit.version",
          "testcontainers.version",
          "jakarta.inject.version",
          "jakarta.persistence.version",
          "jakarta.transaction.version",
          "jakarta.validation.version",
          "postgresql.version",
          "keycloak.version",
          "lombok.version",
          "mapstruct.version",
          "lombok-mapstruct-binding.version",
          "quarkus.platform.version",
          "spring-boot.version",
          "micronaut.platform.version",
          "maven.clean.version",
          "maven.compiler.version",
          "maven.resources.version",
          "maven.surefire.version",
          "maven.failsafe.version",
          "maven.jar.version",
          "maven.install.version",
          "maven.deploy.version",
          "maven.site.version",
          "maven.source.version",
          "maven.javadoc.version",
          "maven.enforcer.version",
          "maven.dependency.version",
          "jacoco.version",
          "flatten.version",
          "spotless.version",
          "google-java-format.version",
          "cyclonedx.version");

  static final Set<String> MANAGED_DEPENDENCIES =
      Set.of(
          "com.fasterxml.jackson:jackson-bom",
          "org.slf4j:slf4j-bom",
          "org.junit:junit-bom",
          "org.testcontainers:testcontainers-bom",
          "jakarta.inject:jakarta.inject-api",
          "jakarta.persistence:jakarta.persistence-api",
          "jakarta.transaction:jakarta.transaction-api",
          "jakarta.validation:jakarta.validation-api",
          "org.postgresql:postgresql",
          "org.projectlombok:lombok",
          "org.mapstruct:mapstruct",
          "org.mapstruct:mapstruct-processor",
          "org.projectlombok:lombok-mapstruct-binding",
          "io.quarkus.platform:quarkus-bom",
          "org.springframework.boot:spring-boot-dependencies",
          "io.micronaut.platform:micronaut-platform");

  static final Set<String> MANAGED_PLUGINS =
      Set.of(
          "org.apache.maven.plugins:maven-clean-plugin",
          "org.apache.maven.plugins:maven-compiler-plugin",
          "org.apache.maven.plugins:maven-resources-plugin",
          "org.apache.maven.plugins:maven-surefire-plugin",
          "org.apache.maven.plugins:maven-failsafe-plugin",
          "org.apache.maven.plugins:maven-jar-plugin",
          "org.apache.maven.plugins:maven-install-plugin",
          "org.apache.maven.plugins:maven-deploy-plugin",
          "org.apache.maven.plugins:maven-site-plugin",
          "org.apache.maven.plugins:maven-source-plugin",
          "org.apache.maven.plugins:maven-javadoc-plugin",
          "org.apache.maven.plugins:maven-enforcer-plugin",
          "org.apache.maven.plugins:maven-dependency-plugin",
          "org.jacoco:jacoco-maven-plugin",
          "org.codehaus.mojo:flatten-maven-plugin",
          "com.diffplug.spotless:spotless-maven-plugin",
          "org.cyclonedx:cyclonedx-maven-plugin");

  private PlatformPolicy() {}
}
