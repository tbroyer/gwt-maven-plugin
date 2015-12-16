package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * Copies all test-source files as if they were test resources (with no filtering or relocation).
 */
@Mojo(name = "import-test-sources", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true)
public class ImportTestSourcesMojo extends AbstractImportSourcesMojo {

  @Parameter(defaultValue = "${project.testCompileSourceRoots}", required = true, readonly = true)
  private List<String> sourceRoots;

  @Parameter(defaultValue = "${project.testResources}", required = true, readonly = true)
  private List<Resource> resources;

  @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
  private File outputDirectory;

  @Override
  protected List<String> getSourceRoots() {
    return sourceRoots;
  }

  @Override
  protected List<Resource> getProjectResources() {
    return resources;
  }

  @Override
  protected File getOutputDirectory() {
    return outputDirectory;
  }
}
