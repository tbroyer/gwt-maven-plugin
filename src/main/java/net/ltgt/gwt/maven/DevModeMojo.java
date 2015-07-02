package net.ltgt.gwt.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * Runs GWT's DevMode.
 */
@Mojo(name = "devmode", requiresDependencyResolution = ResolutionScope.COMPILE, requiresDirectInvocation = true, threadSafe = true, aggregator = true)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class DevModeMojo extends AbstractDevModeMojo {

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/devmode/work")
  private File devmodeWorkDir;

  /**
   * Directory into which deployable output files will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/devmode/war")
  private File warDir;

  /**
   * Additional arguments to be passed to the GWT compiler.
   */
  @Parameter
  private List<String> devmodeArgs;

  @Override
  protected String getMainClass() {
    return "com.google.gwt.dev.DevMode";
  }

  @Override
  protected File getWorkDir() {
    return devmodeWorkDir;
  }

  @Override
  protected Collection<String> getSpecificArguments(Set<String> sources) {
    ArrayList<String> args = new ArrayList<>(2 + (devmodeArgs == null ? 0 : devmodeArgs.size() * 2));
    args.add("-war");
    args.add(warDir.getAbsolutePath());
    if (devmodeArgs != null) {
      args.addAll(devmodeArgs);
    }
    return args;
  }

  @Override
  protected boolean prependSourcesToClasspath() {
    return true;
  }

  @Override
  protected void forceMkdirs() throws IOException {
    FileUtils.forceMkdir(warDir);
  }
}
