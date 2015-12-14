package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;

/**
 * Adds all source directories as resources.
 */
@Mojo(name = "import-sources", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class ImportSourcesMojo extends AbstractImportSourcesMojo {

  @Override
  protected List<String> getSourceRoots() {
    return project.getCompileSourceRoots();
  }

  @Override
  protected void addResource(Resource resource) {
    getLog().info("Adding project.resource " + resource);
    project.addResource(resource);
  }

  @Override
  protected List<Resource> getProjectResources() {
    return project.getResources();
  }
}
