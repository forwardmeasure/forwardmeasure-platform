package com.forwardmeasure.build.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.junit.jupiter.api.Test;

class PolicyInspectorTest {
  private final PolicyInspector inspector = new PolicyInspector();

  @Test
  void acceptsComponentOwnedVersions() {
    Model model = new Model();
    Properties properties = new Properties();
    properties.setProperty("pekko.version", "1.2.0");
    model.setProperties(properties);
    assertTrue(inspector.inspect(model).isEmpty());
  }

  @Test
  void rejectsKeycloakVersionProperty() {
    Model model = new Model();
    Properties properties = new Properties();
    properties.setProperty("keycloak.version", "0.0.0");
    model.setProperties(properties);
    assertEquals(1, inspector.inspect(model).size());
  }

  @Test
  void rejectsPlatformOwnedPropertyDependencyAndPluginVersions() {
    Model model = new Model();
    Properties properties = new Properties();
    properties.setProperty("jackson.version", "0.0.0");
    model.setProperties(properties);
    Dependency dependency = new Dependency();
    dependency.setGroupId("org.postgresql");
    dependency.setArtifactId("postgresql");
    dependency.setVersion("0.0.0");
    model.addDependency(dependency);
    Plugin plugin = new Plugin();
    plugin.setGroupId("org.apache.maven.plugins");
    plugin.setArtifactId("maven-compiler-plugin");
    plugin.setVersion("0.0.0");
    Build build = new Build();
    build.addPlugin(plugin);
    model.setBuild(build);
    assertEquals(3, inspector.inspect(model).size());
  }
}
