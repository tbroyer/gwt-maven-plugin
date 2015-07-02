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
 * Runs GWT's DevMode.
 */
@Mojo(name = "devmode", requiresDependencyResolution = ResolutionScope.COMPILE, requiresDirectInvocation = true, threadSafe = true, aggregator = true)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class DevModeMojo extends AbstractDevModeMojo {

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/devmode")
  private File devmodeWorkDir;

  /**
   * Directory into which deployable output files will be written.
   */
  @Parameter(property = "webappDirectory", required = true)
  private File webappDirectory;

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
    args.add(webappDirectory.getAbsolutePath());
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
    FileUtils.forceMkdir(webappDirectory);
  }
}
