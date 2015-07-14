package net.ltgt.gwt.maven;

import java.util.List;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractImportSourcesMojo extends AbstractSourcesAsResourcesMojo {

  @Override
  public void execute() throws MojoExecutionException {
    // Add the compile source roots as resources to the build
    for (String sourceRoot : getSourceRoots()) {
      addResource(sourceRoot);
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
}
