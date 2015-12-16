package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

/**
 * Copies all source files as if they were resources (with no filtering or relocation).
 */
@Mojo(name = "import-sources", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public class ImportSourcesMojo extends AbstractImportSourcesMojo {

  @Parameter(defaultValue = "${project.compileSourceRoots}", required = true, readonly = true)
  private List<String> sourceRoots;

  @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
  private List<Resource> resources;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
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
