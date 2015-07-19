package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.List;

/**
 * Adds all test-source directories as test resources.
 */
@Mojo(name = "import-test-sources", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES, threadSafe = true)
public class ImportTestSourcesMojo extends AbstractImportSourcesMojo {

  @Override
  protected List<String> getSourceRoots() {
    return project.getTestCompileSourceRoots();
  }

  @Override
  protected void addResource(Resource resource) {
    getLog().info("Adding project.testResource " + resource);
    project.addTestResource(resource);
  }

  @Override
  protected List<Resource> getProjectResources() {
    return project.getTestResources();
  }
}
