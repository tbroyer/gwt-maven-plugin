package net.ltgt.gwt.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Runs GWT's CodeServer (SuperDevMode).
 */
@Mojo(name = "codeserver", requiresDependencyResolution = ResolutionScope.COMPILE, requiresDirectInvocation = true, threadSafe = true, aggregator = true)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class CodeServerMojo extends AbstractDevModeMojo {

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/codeserver")
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
    ArrayList<String> args = new ArrayList<>(3 + (codeserverArgs == null ? 0 : codeserverArgs.size()) + sources.size() * 2);
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
