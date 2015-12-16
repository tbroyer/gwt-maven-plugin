package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractAddSuperSourcesMojo extends AbstractSourcesAsResourcesMojo {

  /**
   * Name of the module into which to optionally relocate super-sources.
   * <p>
   * Super-sources will be relocated into a {@code super} subfolder.
   */
  @Parameter
  protected String moduleName;

  public AbstractAddSuperSourcesMojo() {
    super();
  }

  @Override
  public void execute() throws MojoExecutionException {
    String superSourceRoot = getSuperSourceRoot();
    if (checkResource(superSourceRoot)) {
      Resource resource = createResource(superSourceRoot);
      if (isSuperSourceRelocated()) {
        if (StringUtils.isBlank(moduleName)) {
          throw new MojoExecutionException("Cannot relocate super-sources if moduleName is not specified");
        }
        String targetPath = moduleName.replace('.', '/');
        // Keep only package name
        targetPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
        // Relocate into 'super' subfolder
        targetPath = ensureTrailingSlash(targetPath) + "super/";
        resource.setTargetPath(targetPath);
      }
      addResource(resource);
    }
  }

  protected abstract String getSuperSourceRoot();

  protected abstract boolean isSuperSourceRelocated();

  protected abstract void addResource(Resource resource);
}
