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

  @Override
  public void execute() throws MojoFailureException {
    List<String> violations = new PolicyInspector().inspect(project.getOriginalModel());
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
