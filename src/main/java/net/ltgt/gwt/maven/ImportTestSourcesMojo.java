package net.ltgt.gwt.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

/**
 * Adds all test-source directories as resources, and unpacks and add as resource each test dependency with {@code <type>java-source</type>}.
 */
@Mojo(name = "import-test-sources", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class ImportTestSourcesMojo extends AbstractImportSourcesMojo {

  /**
   * Location where java-source dependencies will be unpacked.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-resources/gwt-test-sources", required = true)
  private File importedTestSourcesTargetDirectory;

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
  protected File getOutputDirectory() {
    return importedTestSourcesTargetDirectory;
  }

  @Override
  protected String getSuperSourceRoot() {
    return testSuperSourceDirectory;
  }

  @Override
  protected boolean isSuperSourceRelocated() {
    return relocateTestSuperSource;
  }

  @Override
  protected List<String> getSourceRoots() {
    return project.getTestCompileSourceRoots();
  }

  protected boolean includeArtifact(Artifact artifact) {
    return Artifact.SCOPE_TEST.equals(artifact.getScope());
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
