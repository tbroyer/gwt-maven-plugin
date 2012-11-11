package net.ltgt.gwt.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.UnArchiver;

import java.io.File;

public abstract class AbstractImportSourcesMojo extends AbstractMojo {

  @Component
  protected MavenProject project;

  @Component
  private MavenProjectHelper projectHelper;

  @Component(hint = "jar")
  private UnArchiver unArchiver;

  public AbstractImportSourcesMojo() {
    super();
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // Add the compile source roots as resources to the build
    for (String sourceRoot : getSourceRoots()) {
      projectHelper.addResource(project, sourceRoot, null, null);
    }
  
    // Now unpack the type=java-source dependencies and add them as resources
    unArchiver.setDestDirectory(getOutputDirectory());
  
    for (Artifact artifact : project.getDependencyArtifacts()) {
      if (!includeArtifact(artifact)) {
        continue;
      }
      if (!"java-source".equals(artifact.getArtifactHandler().getPackaging())) {
        if (getLog().isDebugEnabled()) {
          getLog().debug("Skipping non-java-source dependency: " + artifact.getId());
        }
        continue;
      }
      // Check copied from maven-dependency-plugin.
      // TODO: copy directory reursively, while dealing with http://jira.codehaus.org/browse/MNG-5214
      if (artifact.getFile().isDirectory()) {
        // usual case is a future jar packaging, but there are special cases: classifier and other packaging
        throw new MojoExecutionException(artifact.getId() + " has not been packaged yet. When used on reactor artifact, "
            + "import-sources should be executed after packaging.");
      }
      unArchiver.setSourceFile(artifact.getFile());
      // Defer outputDirectory creation so that it's only tentatively created if there are source JARs to unpack
      ensureOutputDirectory();
      unArchiver.extract();
    }
  }

  protected abstract Iterable<String> getSourceRoots();

  protected abstract File getOutputDirectory();

  protected abstract boolean includeArtifact(Artifact artifact);

  private void ensureOutputDirectory() throws MojoExecutionException {
    if (!getOutputDirectory().exists() && !getOutputDirectory().mkdirs()) {
      throw new MojoExecutionException("Cannot create output directory: " + getOutputDirectory().getAbsolutePath());
    }
    projectHelper.addResource(project, getOutputDirectory().getPath(), null, null);
  }
}