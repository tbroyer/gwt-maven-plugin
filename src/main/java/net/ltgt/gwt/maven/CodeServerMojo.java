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
 * Runs GWT's CodeServer (SuperDevMode).
 */
@Mojo(name = "codeserver", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true, aggregator = true)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class CodeServerMojo extends AbstractDevModeMojo {

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/codeserver", required = true)
  private File codeserverWorkDir;

  /**
   * Directory where files for launching SuperDevMode (e.g. {@code *.nocache.js}) will be written. (Optional.)
   */
  @Parameter(property = "launcherDir")
  private File launcherDir;

  /**
   * Additional arguments to be passed to the GWT compiler.
   */
  @Parameter
  private List<String> codeserverArgs;

  @Override
  protected String getMainClass() {
    return "com.google.gwt.dev.codeserver.CodeServer";
  }

  @Override
  protected File getWorkDir() {
    return codeserverWorkDir;
  }

  @Override
  protected Collection<String> getSpecificArguments(Set<String> sources) {
    ArrayList<String> args = new ArrayList<>(4 + (codeserverArgs == null ? 0 : codeserverArgs.size()) + sources.size() * 2);
    if (launcherDir != null) {
      args.add("-launcherDir");
      args.add(launcherDir.getAbsolutePath());
    }
    if (codeserverArgs != null) {
      args.addAll(codeserverArgs);
    }
    args.add("-allowMissingSrc");
    for (String src : sources) {
      args.add("-src");
      args.add(src);
    }
    return args;
  }

  @Override
  protected void forceMkdirs() throws IOException {
    if (launcherDir != null) {
      FileUtils.forceMkdir(launcherDir);
    }
  }
}
