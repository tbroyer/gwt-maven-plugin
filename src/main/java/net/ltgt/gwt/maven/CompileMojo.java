package net.ltgt.gwt.maven;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * Invokes the GWT Compiler on the project's sources and resources.
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
@SuppressWarnings("serial")
public class CompileMojo extends AbstractMojo implements GwtOptions {

  /**
   * Enable faster, but less-optimized, compilations.
   */
  @Parameter(property = "gwt.draftCompile", defaultValue = "false")
  private boolean draftCompile;

  /**
   * The directory into which deployable but not servable output files will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/deploy")
  private File deploy;

  /**
   * The directory into which extra files, not intended for deployment, will be written.
   */
  @Parameter
  private File extra;

  /**
   * The number of local workers to use when compiling permutations.
   */
  @Parameter(property = "gwt.localWorkers")
  private int localWorkers;

  /**
   * Sets the level of logging detail. Defaults to Maven's own log level.
   */
  @Parameter(property = "gwt.logLevel")
  private LogLevel logLevel;

  /**
   * Name of the module to compile.
   */
  @Parameter(required = true)
  private String moduleName;

  /**
   * The short name of the module, used to name the output {@code .nocache.js} file.
   */
  @Parameter
  private String moduleShortName;

  /**
   * Sets the optimization level used by the compiler.  0=none 9=maximum.
   */
  @Parameter(property = "gwt.optimize", defaultValue = "9")
  private int optimize;

  /**
   * Specifies Java source level.
   */
  @Parameter(property = "maven.compiler.source")
  private String sourceLevel;

  /**
   * Script output style: OBFUSCATED, PRETTY, or DETAILED.
   */
  @Parameter(property = "gwt.style", defaultValue = "OBFUSCATED")
  private Style style;

  /**
   * Only succeed if no input files have errors.
   */
  @Parameter(property = "gwt.failOnError", defaultValue = "false")
  private boolean failOnError;

  /**
   * Specifies the location of the target war directory.
   */
  @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
  private File webappDirectory;

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/work")
  private File workDir;

  /**
   * Additional arguments to be passed to the GWT compiler.
   */
  @Parameter
  private List<String> compilerArgs;

  /**
   * Arguments to be passed to the forked JVM (e.g. {@code -Xmx})
   */
  @Parameter
  private List<String> jvmArgs;

  /**
   * List of system properties to pass to the GWT compiler.
   */
  @Parameter
  private Map<String, String> systemProperties;

  /**
   * Sets the granularity in milliseconds of the last modification
   * date for testing whether the module needs recompilation.
   */
  @Parameter(property = "lastModGranularityMs", defaultValue = "0")
  private int staleMillis;

  /**
   * Require the GWT plugin to compile the GWT module even if none of the
   * sources appear to have changed. By default, this plugin looks to see if
   * the output *.nocache.js exists and inputs (POM, sources and dependencies)
   * have not changed.
   */
  @Parameter(property = "gwt.forceCompilation", defaultValue="false")
  private boolean forceCompilation;

  /**
   * Require the GWT plugin to skip compilation. This can be useful to quickly
   * package an incomplete or stale application that's used as a dependency (an
   * overlay generally) in a war, for example to launch that war in a container
   * and then launch DevMode for this GWT application.
   */
  @Parameter(property = "gwt.skipCompilation", defaultValue="false")
  private boolean skipCompilation;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    if (skipCompilation) {
      getLog().info("GWT compilation is skipped");
      return;
    }

    if (!forceCompilation && !isStale()) {
      getLog().info("Compilation output seems uptodate. GWT compilation skipped.");
      return;
    }

    List<String> args = new ArrayList<String>();
    if (jvmArgs != null) {
      args.addAll(jvmArgs);
    }
    if (systemProperties != null) {
      for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
        args.add("-D" + entry.getKey() + "=" + entry.getValue());
      }
    }
    args.add("com.google.gwt.dev.Compiler");
    args.addAll(CommandlineBuilder.buildArgs(getLog(), this));
    if (failOnError) {
      args.add("-failOnError");
    }
    if (compilerArgs != null) {
      args.addAll(compilerArgs);
    }
    args.add(moduleName);

    Set<String> cp = new LinkedHashSet<String>();
    try {
      cp.addAll(project.getCompileClasspathElements());
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    Commandline commandline = new Commandline();
    commandline.setWorkingDirectory(project.getBuild().getDirectory());
    commandline.setExecutable(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
    commandline.addEnvironment("CLASSPATH", StringUtils.join(cp.iterator(), File.pathSeparator));
    commandline.addArguments(args.toArray(new String[args.size()]));

    int result;
    try {
      result = CommandLineUtils.executeCommandLine(commandline,
          new StreamConsumer() {
            @Override
            public void consumeLine(String s) {
              getLog().info(s);
            }
          },
          new StreamConsumer() {
            @Override
            public void consumeLine(String s) {
              getLog().warn(s);
            }
          });
    } catch (CommandLineException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    if (result != 0) {
      throw new MojoExecutionException("GWT Compiler exited with status " + result);
    }

    // XXX: workaround for GWT 2.7.0 not setting nocache.js lastModified correctly.
    if (isStale()) {
      final String shortName = getModuleShortName();
      final File nocacheJs = new File(webappDirectory, shortName + File.separator + shortName + ".nocache.js");
      nocacheJs.setLastModified(System.currentTimeMillis());
    }
  }

  private boolean isStale() throws MojoExecutionException {
    if (!webappDirectory.exists()) {
      return true;
    }

    // TODO: take various flags into account
    final String shortName = getModuleShortName();
    final File nocacheJs = new File(webappDirectory, shortName + File.separator + shortName + ".nocache.js");
    if (!nocacheJs.isFile()) {
      getLog().debug(nocacheJs.getPath() + " file found or is not a file: recompiling");
      return true;
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug("Found *.nocache.js at " + nocacheJs.getAbsolutePath());
    }

    StaleSourceScanner scanner = new StaleSourceScanner(staleMillis);
    scanner.addSourceMapping(new SourceMapping() {
      final Set<File> targetFiles = Collections.singleton(nocacheJs);

      @Override
      public Set<File> getTargetFiles(File targetDir, String source) throws InclusionScanException {
        return targetFiles;
      }
    });

    // compiled (processed) classes and resources (incl. processed and generated ones, and sources through gwt:import-sources)
    if (isStale(scanner, new File(project.getBuild().getOutputDirectory()), nocacheJs)) {
      return true;
    }
    // POM
    if (isStale(scanner, project.getFile(), nocacheJs)) {
      return true;
    }
    // dependencies
    ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);
    for (Artifact artifact : project.getArtifacts()) {
      if (!artifactFilter.include(artifact)) {
        continue;
      }
      if (isStale(scanner, artifact.getFile(), nocacheJs)) {
        return true;
      }
    }

    return false;
  }

  private boolean isStale(StaleSourceScanner scanner, File sourceFile, File targetFile) throws MojoExecutionException {
    if (!sourceFile.isDirectory()) {
      boolean stale = (targetFile.lastModified() + staleMillis < sourceFile.lastModified());
      if (stale && getLog().isDebugEnabled()) {
        getLog().debug("Source file is newer than nocache.js, recompiling: " + sourceFile.getAbsolutePath());
      }
      return stale;
    }

    try {
      Set<File> sourceFiles = scanner.getIncludedSources(sourceFile, webappDirectory);
      boolean stale = !sourceFiles.isEmpty();
      if (stale && getLog().isDebugEnabled()) {
        StringBuilder sb = new StringBuilder();
        for (File source : sourceFiles) {
          sb.append("\n - ").append(source.getAbsolutePath());
        }
        getLog().debug("Source files are newer than nocache.js, recompiling: " + sb.toString());
      }
      return stale;
    } catch (InclusionScanException e) {
      throw new MojoExecutionException("Error scanning source root: \'" + sourceFile.getPath() + "\' for stale files to recompile.", e);
    }
  }

  private String getModuleShortName() {
    if (StringUtils.isBlank(moduleShortName)) {
      // TODO: load ModuleDef to get target name
      return moduleName;
    }
    return moduleShortName;
  }

  @Override
  public LogLevel getLogLevel() {
    return logLevel;
  }

  @Override
  public Style getStyle() {
    return style;
  }

  @Override
  public int getOptimize() {
    return optimize;
  }

  @Override
  public File getWarDir() {
    return webappDirectory;
  }

  @Override
  public File getWorkDir() {
    return workDir;
  }

  @Override
  public File getDeployDir() {
    return deploy;
  }

  @Nullable
  @Override
  public File getExtraDir() {
    return extra;
  }

  @Override
  public boolean isDraftCompile() {
    return draftCompile;
  }

  @Override
  public int getLocalWorkers() {
    return localWorkers;
  }

  @Override
  public String getSourceLevel() {
    return sourceLevel;
  }
}
