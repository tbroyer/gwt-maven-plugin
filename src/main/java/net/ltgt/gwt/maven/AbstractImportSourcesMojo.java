package net.ltgt.gwt.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class AbstractImportSourcesMojo extends AbstractMojo {

  private static final List<String> JAVA_SOURCES = Collections.singletonList("**/*.java");

  /**
   * Name of the module into which to optionally relocate super-sources.
   * <p>
   * Super-sources will be relocated into a {@code super} subfolder.
   */
  @Parameter
  protected String moduleName;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject project;

  @Component(hint = "jar")
  private UnArchiver unArchiver;

  public AbstractImportSourcesMojo() {
    super();
  }

  @Override
  public void execute() throws MojoExecutionException {
    // Add super-sources
    // FIXME: should probably be done earlier (initialize, or a lifecycle participant)
    String superSourceRoot = getSuperSourceRoot();
    if (checkResource(superSourceRoot)) {
      Resource resource = createResource(superSourceRoot);
      if (isSuperSourceRelocated()) {
        if (StringUtils.isBlank(moduleName)) {
          throw new MojoExecutionException("Cannot relocate super-sources if moduleName is not specified");
        }
        String targetPath = moduleName.replace('.', '/');
        // Keep only package name
        targetPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
        // Relocate into 'super' subfolder
        targetPath = ensureTrailingSlash(targetPath) + "super/";
        resource.setTargetPath(targetPath);
      }
      addResource(resource);
    }

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

  private boolean checkResource(String sourceRoot) {
    // TODO: cache a processed list of Resources in a ThreadLocal as an optimization?
    sourceRoot = ensureTrailingSlash(sourceRoot);
    for (Resource resource : getProjectResources()) {
      String dir = ensureTrailingSlash(resource.getDirectory());
      if (dir.startsWith(sourceRoot) || sourceRoot.startsWith(dir)) {
        getLog().warn(String.format(
            "Conflicting path between source folder (%s, to be added as resource) and resource (%s); skipping.",
            sourceRoot, dir));
        return false;
      }
    }
    return true;
  }

  private Resource createResource(String resourceDirectory) {
    Resource resource = new Resource();
    resource.setDirectory(resourceDirectory);
    resource.setIncludes(JAVA_SOURCES);
    return resource;
  }

  private void addResource(String resourceDirectory) {
    if (checkResource(resourceDirectory)) {
      Resource resource = createResource(resourceDirectory);
      addResource(resource);
    }
  }

  protected abstract List<Resource> getProjectResources();

  protected abstract void addResource(Resource resource);

  protected abstract String getSuperSourceRoot();

  protected abstract boolean isSuperSourceRelocated();

  protected abstract Iterable<String> getSourceRoots();

  protected abstract File getOutputDirectory();

  protected abstract boolean includeArtifact(Artifact artifact);

  private String ensureTrailingSlash(String directory) {
    if (directory.endsWith("/")) {
      return directory;
    }
    return directory + "/";
  }
}