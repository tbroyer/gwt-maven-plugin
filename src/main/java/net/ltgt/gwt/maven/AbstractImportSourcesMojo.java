package net.ltgt.gwt.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractImportSourcesMojo extends AbstractSourcesAsResourcesMojo {

  @Component(hint = "jar")
  private UnArchiver unArchiver;

  @Override
  public void execute() throws MojoExecutionException {
    // Add the compile source roots as resources to the build
    for (String sourceRoot : getSourceRoots()) {
      addResource(sourceRoot);
    }

    // Now unpack the type=java-source dependencies and add them as resources
    if (!getOutputDirectory().exists() && !getOutputDirectory().mkdirs()) {
      throw new MojoExecutionException("Cannot create output directory: " + getOutputDirectory().getAbsolutePath());
    }
    unArchiver.setDestDirectory(getOutputDirectory());
    addResource(getOutputDirectory().getPath());

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
      // copy directory recursively, while dealing with http://jira.codehaus.org/browse/MNG-5214
      if (artifact.getFile().isDirectory()) {
        // usual case is a future jar packaging, but there are special cases: classifier and other packaging
        getLog().warn(artifact.getId() + " has not been packaged yet, trying to infer sources from reactor.");
        importFromProjectReferences(ArtifactUtils.key(artifact));
      } else {
        unArchiver.setSourceFile(artifact.getFile());
        unArchiver.extract();
      }
    }
  }

  private void importFromProjectReferences(String id) throws MojoExecutionException {
    try {
      MavenProject reference = project.getProjectReferences().get(id);
      for (String sourceRoot : reference.getCompileSourceRoots()) {
        File sourceDirectory = new File(sourceRoot);
        if (!sourceDirectory.isAbsolute()) {
          sourceDirectory = new File(reference.getBasedir(), sourceRoot);
        }
        if (sourceDirectory.exists()) {
          getLog().info("copying " + sourceDirectory);
          FileUtils.copyDirectoryStructureIfModified(sourceDirectory, getOutputDirectory());
        } else {
          getLog().info("skip non existing imported source directory " + sourceDirectory);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void addResource(String resourceDirectory) {
    if (checkResource(resourceDirectory)) {
      Resource resource = createResource(resourceDirectory);
      addResource(resource);
    }
  }

  protected abstract List<Resource> getProjectResources();

  protected abstract void addResource(Resource resource);

  protected abstract Iterable<String> getSourceRoots();

  protected abstract File getOutputDirectory();

  protected abstract boolean includeArtifact(Artifact artifact);
}
