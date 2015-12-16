package net.ltgt.gwt.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

public abstract class AbstractImportSourcesMojo extends AbstractSourcesAsResourcesMojo {
  /**
   * The character encoding scheme to be applied when copying files.
   */
  @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
  private String encoding;

  @Component( role = MavenResourcesFiltering.class, hint = "default" )
  private MavenResourcesFiltering mavenResourcesFiltering;

  @Parameter( defaultValue = "${session}", readonly = true, required = true )
  private MavenSession session;

  /**
   * Overwrite existing files even if the destination files are newer.
   */
  @Parameter( property = "maven.resources.overwrite", defaultValue = "false" )
  private boolean overwrite;

  @Override
  public void execute() throws MojoExecutionException {
    List<Resource> resources = new ArrayList<>();
    for (String sourceRoot : getSourceRoots()) {
      if (checkResource(sourceRoot)) {
        resources.add(createResource(sourceRoot));
      }
    }
    MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
        resources, getOutputDirectory(), project, encoding, null, Collections.<String>emptyList(), session);
    mavenResourcesExecution.setInjectProjectBuildFilters(false);
    mavenResourcesExecution.setOverwrite(overwrite);
    mavenResourcesExecution.setIncludeEmptyDirs(false);
    mavenResourcesExecution.setFilterFilenames(false);
    try {
      mavenResourcesFiltering.filterResources(mavenResourcesExecution);
    } catch (MavenFilteringException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  protected abstract List<Resource> getProjectResources();

  protected abstract Iterable<String> getSourceRoots();

  protected abstract File getOutputDirectory();
}
