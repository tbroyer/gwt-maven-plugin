package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Collections;
import java.util.List;

public abstract class AbstractSourcesAsResourcesMojo extends AbstractMojo {
  private static final List<String> JAVA_SOURCES = Collections.singletonList("**/*.java");

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject project;

  protected boolean checkResource(String sourceRoot) {
    // TODO: cache a processed list of Resources in a ThreadLocal as an optimization?
    sourceRoot = ensureTrailingSlash(sourceRoot);
    for (Resource resource : getProjectResources()) {
      String dir = ensureTrailingSlash(resource.getDirectory());
      if (dir.equals(sourceRoot)) {
        getLog().info(sourceRoot + " already added as a resource folder; skipping.");
        continue;
      }
      if (dir.startsWith(sourceRoot) || sourceRoot.startsWith(dir)) {
        getLog().warn(String.format(
            "Conflicting path between source folder (%s, to be added as resource) and resource (%s); skipping.",
            sourceRoot, dir));
        return false;
      }
    }
    return true;
  }

  protected Resource createResource(String resourceDirectory) {
    Resource resource = new Resource();
    resource.setDirectory(resourceDirectory);
    resource.setIncludes(JAVA_SOURCES);
    return resource;
  }

  protected abstract List<Resource> getProjectResources();

  protected abstract void addResource(Resource resource);

  protected String ensureTrailingSlash(String directory) {
    if (directory.endsWith("/")) {
      return directory;
    }
    return directory + "/";
  }
}
