package net.ltgt.gwt.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Mojo(name = "add-super-sources", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class AddSuperSourceMojo extends AbstractAddSuperSourcesMojo {

  /**
   * Directory containing super-sources.
   */
  @Parameter(defaultValue = "src/main/super")
  private String superSourceDirectory;

  /**
   * Whether to relocate {@code superSourceDirectory} content within the
   * module given in {@code moduleName}.
   * <p>
   * Super-sources will be relocated into a {@code super} subfolder.
   */
  @Parameter(defaultValue = "false")
  private boolean relocateSuperSource;

  @Override
  protected String getSuperSourceRoot() {
    return superSourceDirectory;
  }

  @Override
  protected boolean isSuperSourceRelocated() {
    return relocateSuperSource;
  }

  @Override
  protected void addResource(Resource resource) {
    project.addResource(resource);
  }

  @Override
  protected List<Resource> getProjectResources() {
    return project.getResources();
  }
}
