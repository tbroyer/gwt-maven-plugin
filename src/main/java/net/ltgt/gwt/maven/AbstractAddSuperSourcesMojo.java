package net.ltgt.gwt.maven;

import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractAddSuperSourcesMojo extends AbstractMojo {

  /**
   * Name of the module into which to optionally relocate super-sources.
   * <p>
   * Super-sources will be relocated into a {@code super} subfolder.
   */
  @Parameter
  protected String moduleName;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    for (String superSourceRoot : SourcesAsResourcesHelper.filterSourceRoots(
        getLog(), getProjectResources(), Collections.singleton(getSuperSourceRoot()))) {
      Resource resource = new Resource();
      resource.setDirectory(superSourceRoot);
      if (isSuperSourceRelocated()) {
        if (StringUtils.isBlank(moduleName)) {
          throw new MojoExecutionException("Cannot relocate super-sources if moduleName is not specified");
        }
        String targetPath = moduleName.replace('.', '/');
        // Keep only package name
        targetPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
        // Relocate into 'super' subfolder
        targetPath = SourcesAsResourcesHelper.ensureTrailingSlash(targetPath) + "super/";
        resource.setTargetPath(targetPath);
      }
      addResource(resource);
    }
  }

  protected abstract String getSuperSourceRoot();

  protected abstract boolean isSuperSourceRelocated();

  protected abstract void addResource(Resource resource);

  protected abstract List<Resource> getProjectResources();
}
