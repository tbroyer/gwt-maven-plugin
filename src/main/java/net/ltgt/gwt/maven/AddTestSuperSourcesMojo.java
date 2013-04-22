package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Mojo(name = "add-test-super-sources", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class AddTestSuperSourcesMojo extends AbstractAddSuperSourcesMojo {

  /**
   * Directory containing super-sources for tests.
   */
  @Parameter(defaultValue = "src/test/super")
  private String testSuperSourceDirectory;

  /**
   * Whether to relocate {@code testSuperSourceDirectory} content within the
   * module given in {@code moduleName}.
   * <p>
   * Super-sources will be relocated into a {@code super} subfolder.
   */
  @Parameter(defaultValue = "false")
  private boolean relocateTestSuperSource;

  @Override
  protected String getSuperSourceRoot() {
    return testSuperSourceDirectory;
  }

  @Override
  protected boolean isSuperSourceRelocated() {
    return relocateTestSuperSource;
  }

  @Override
  protected void addResource(Resource resource) {
    project.addTestResource(resource);
  }

  @Override
  protected List<Resource> getProjectResources() {
    return project.getTestResources();
  }
}
