package net.ltgt.gwt.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Enforces that the source encoding is UTF-8.
 */
@Mojo(name = "enforce-encoding", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class EnforceEncodingMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String encoding = project.getProperties().getProperty("project.build.sourceEncoding");
    if (encoding == null) {
      getLog().info("Setting project.build.sourceEncoding to UTF-8");
    } else if (!encoding.equalsIgnoreCase("UTF-8")) {
      getLog().warn("Encoding was set to " + encoding + "; forcing it to UTF-8");
    } else {
      getLog().info("Project already has UTF-8 encoding");
    }
    project.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");
  }
}
