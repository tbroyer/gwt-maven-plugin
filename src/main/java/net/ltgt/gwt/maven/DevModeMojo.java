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
import org.codehaus.plexus.util.FileUtils;

/**
 * Runs GWT's DevMode.
 */
@Mojo(name = "devmode", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true, aggregator = true)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class DevModeMojo extends AbstractDevModeMojo {

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/devmode/work", required = true)
  private File devmodeWorkDir;

  /**
   * Directory into which deployable output files will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/devmode/war", required = true)
  private File warDir;

  /**
   * Automatically launches the specified URLs.
   */
  @Parameter
  private List<String> startupUrls;

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
    ArrayList<String> args = new ArrayList<>(2 + (startupUrls == null ? 0 : startupUrls.size() * 2) + (devmodeArgs == null ? 0 : devmodeArgs.size()));
    args.add("-war");
    args.add(warDir.getAbsolutePath());
    if (startupUrls != null) {
      for (String startupUrl : startupUrls) {
        args.add("-startupUrl");
        args.add(startupUrl);
      }
    }
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
