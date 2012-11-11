package net.ltgt.gwt.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

/**
 * Adds all source directories as resources, and unpacks and add as resource each compile+runtime dependency with {@code <type>java-source</type>}.
 */
@Mojo(name = "import-sources", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ImportSourcesMojo extends AbstractImportSourcesMojo {

  /**
   * Location where java-source dependencies will be unpacked.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-resources/gwt-sources", required = true)
  private File importedSourcesTargetDirectory;

  private final ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME_PLUS_SYSTEM);

  @Override
  protected File getOutputDirectory() {
    return importedSourcesTargetDirectory;
  }

  @Override
  protected List<String> getSourceRoots() {
    return project.getCompileSourceRoots();
  }

  @Override
  protected boolean includeArtifact(Artifact artifact) {
    return artifactFilter.include(artifact);
  }
}
