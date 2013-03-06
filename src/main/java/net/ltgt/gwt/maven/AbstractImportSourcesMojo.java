package net.ltgt.gwt.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class AbstractImportSourcesMojo extends AbstractMojo {

  private static final List<String> JAVA_SOURCES = Collections.singletonList("**/*.java");

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
  public void execute() throws MojoExecutionException {
    // Add super-sources
    // FIXME: should probably be done earlier (initialize, or a lifecycle participant)
    addResource(getSuperSourceRoot());

    // Add the compile source roots as resources to the build
    for (String sourceRoot : getSourceRoots()) {
      addResource(sourceRoot);
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
      if (getLog().isInfoEnabled()) {
        getLog().info("Importing " + artifact.getId());
      }
      // TODO: copy directory recursively, while dealing with http://jira.codehaus.org/browse/MNG-5214
      if (artifact.getFile().isDirectory()) {
        // usual case is a future jar packaging, but there are special cases: classifier and other packaging
        getLog().warn(artifact.getId() + " has not been packaged yet, trying to infer sources from reactor.");
        importFromProjectReferences(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getBaseVersion());
      } else {
        unArchiver.setSourceFile(artifact.getFile());
        // Defer outputDirectory creation so that it's only tentatively created if there are source JARs to unpack
        ensureOutputDirectory();
        unArchiver.extract();
      }
    }
  }

  private void importFromProjectReferences(String id) throws MojoExecutionException {
    try {
      MavenProject reference = project.getProjectReferences().get(id);
      for (String sourceRoot : reference.getCompileSourceRoots()) {
        File sourceDirectory = new File(reference.getBasedir(), sourceRoot);
        FileUtils.copyDirectoryStructureIfModified(sourceDirectory, getOutputDirectory());
      }
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  protected void addResource(String sourceRoot) {
    // TODO: cache a processed list of Resources in a ThreadLocal as an optimization?
    sourceRoot = ensureTrailingSlash(sourceRoot);
    for (Resource resource : getProjectResources()) {
      String dir = ensureTrailingSlash(resource.getDirectory());
      if (dir.startsWith(sourceRoot) || sourceRoot.startsWith(dir)) {
        getLog().warn(String.format(
            "Conflicting path between source folder (%s, to be added as resource) and resource (%s); skipping.",
            sourceRoot, dir));
        return;
      }
    }
    addResource(projectHelper, project, sourceRoot, JAVA_SOURCES, null);
  }

  protected abstract void addResource(MavenProjectHelper projectHelper, MavenProject project, String sourceRoot, List<String> includes, List<String> excludes);

  protected abstract List<Resource> getProjectResources();

  protected abstract String getSuperSourceRoot();

  protected abstract Iterable<String> getSourceRoots();

  protected abstract File getOutputDirectory();

  protected abstract boolean includeArtifact(Artifact artifact);

  private String ensureTrailingSlash(String directory) {
    if (directory.endsWith("/")) {
      return directory;
    }
    return directory + "/";
  }

  private void ensureOutputDirectory() throws MojoExecutionException {
    if (!getOutputDirectory().exists() && !getOutputDirectory().mkdirs()) {
      throw new MojoExecutionException("Cannot create output directory: " + getOutputDirectory().getAbsolutePath());
    }
    addResource(getOutputDirectory().getPath());
  }
}