package net.ltgt.gwt.maven;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.PermutationWorkerFactory;
import com.google.gwt.dev.ThreadedPermutationWorkerFactory;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.jjs.JsOutputOption;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

@Mojo(name = "compile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyCollection = ResolutionScope.COMPILE)
@SuppressWarnings("serial")
public class CompileMojo extends AbstractMojo implements CompilerOptions {

  enum CompileReport {
    OFF,
    ON,
    HTML,
    DETAILED,
    DETAILED_HTML
  }

  /**
   * Enable faster, but less-optimized, compilations.
   */
  @Parameter(property = "gwt.draftCompile", defaultValue = "false")
  private boolean draftCompile;

  /**
   * Troubleshooting: Prevent the Production Mode compiler from performing aggressive optimizations.
   */
  @Parameter(property = "gwt.disableAggressiveOptimization", defaultValue = "false")
  private boolean disableAggressiveOptimization;

  /**
   * Enable CompilerMetrics.
   */
  @Parameter(property = "gwt.compilerMetrics", defaultValue = "false")
  private boolean compilerMetrics;

  /**
   * The directory into which deployable but not servable output files will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/extra")
  private File deploy;

  /**
   * EXPERIMENTAL: Disables run-time checking of cast operations.
   */
  @Parameter(property = "gwt.disableCastChecking", defaultValue = "false")
  private boolean disableCastChecking;

  /**
   * EXPERIMENTAL: Disables some {@code java.lang.Class} methods (e.g. {@code getName()}).
   */
  @Parameter(property = "gwt.disableClassMetadata", defaultValue = "false")
  private boolean disableClassMetadata;

  /**
   * Disable the check to see if an update version of GWT is available.
   */
  @Parameter(property = "gwt.disableUpdateCheck", defaultValue = "false")
  private boolean disableUpdateCheck;

  /**
   * Debugging: causes the compiled output to check assert statements.
   */
  @Parameter(property = "gwt.enableAssertions", defaultValue = "false")
  private boolean enableAssertions;

  /**
   * EXPERIMENTAL: Enables Closure Compiler optimizations.
   */
  @Parameter(property = "gwt.enableClosureCompiler", defaultValue = "false")
  private boolean enableClosureCompiler;

  /**
   * Disables running generators on CompilePerms shards, even when it would be a likely speedup.
   */
  @Parameter(property = "gwt.disableGeneratingOnShards", defaultValue = "false")
  private boolean disableGeneratingOnShards;

  /**
   * The directory into which extra files, not intended for deployment, will be written.
   */
  @Parameter(property = "gwt.extra", defaultValue = "${project.build.directory}/gwt/extra")
  private File extra;

  /**
   * EXPERIMENTAL: Limits of number of fragments using a code splitter that merges split points.
   */
  @Parameter(property = "gwt.fragmentCount", defaultValue = "-1")
  private int fragmentCount;

  /**
   * Debugging: causes normally-transient generated types to be saved in the specified directory.
   */
  @Parameter(property = "gwt.gen", defaultValue = "${project.build.directory}/gwt/gen")
  private File gen;

  /**
   * The number of local workers to use when compiling permutations.
   */
  @Parameter(property = "gwt.localWorkers")
  private int localWorkers;

  /**
   * Sets the level of logging detail. Defaults to Maven's own log level.
   * <p>
   * If this is set lower than what's loggable at the Maven level, then lower
   * levels will be log at Maven's lowest logging level. For instance, if this
   * is set to {@link TreeLogger.Type.INFO INFO} and Maven has been run in
   * quiet mode (showing only errors), then warnings and informational messages
   * emitted by GWT will actually be logged as errors by the plugin.
   */
  @Parameter(property = "gwt.logLevel")
  private TreeLogger.Type logLevel;

  /**
   * Name of the module to compile.
   */
  @Parameter(required = true)
  private String moduleName;

  /**
   * Sets the optimization level used by the compiler.  0=none 9=maximum.
   */
  @Parameter(property = "gwt.optimize", defaultValue = "" + OPTIMIZE_LEVEL_MAX)
  private int optimize;

  /** This is set and read by the compiler. */
  private boolean optimizePrecompile = true;

  /**
   * Disable runAsync code-splitting.
   */
  @Parameter(property = "gwt.disableRunAsync", defaultValue = "false")
  private boolean disableRunAsync;

  /**
   * Script output style: OBFUSCATED, PRETTY, or DETAILED.
   */
  @Parameter(property = "gwt.style", defaultValue = "OBFUSCATED")
  private JsOutputOption style;

  /**
   * Which kind of compile report to produce: OFF, ON, HTML, DETAILED or DETAILED_HTML.
   */
  @Parameter(property = "gwt.compileReport", defaultValue = "OFF")
  private CompileReport compileReport;

  /**
   * Only succeed if no input files have errors.
   */
  @Parameter(property = "gwt.strict", defaultValue = "false")
  private boolean strict;

  /**
   * Validate all source code, but do not compile.
   */
  @Parameter(property = "gwt.validateOnly", defaultValue = "false")
  private boolean validateOnly;

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
   * Require the GWT plugin to skip compilation. This can be useful to quickly
   * package an incomplete or stale application that's used as a dependency (an
   * overlay generally) in a war, for example to launch that war in a container
   * and then launch DevMode for this GWT application.
   */
  @Parameter(property = "gwt.skipCompilation", defaultValue="false")
  private boolean skipCompilation;

  // TODO: speedtracer

  @Component
  private MavenProject project;

  @Parameter(defaultValue = "${plugin.artifacts}", required = true, readonly = true)
  private List<Artifact> pluginArtifacts;

  public void execute() throws MojoExecutionException {
    if (skipCompilation) {
      getLog().info("GWT compilation is skipped");
      return;
    }

    if (localWorkers < 1) {
      localWorkers = Runtime.getRuntime().availableProcessors();
      if (getLog().isDebugEnabled()) {
        getLog().debug("Using " + localWorkers + " local workers");
      }
    }
    if (draftCompile) {
      optimize = OPTIMIZE_LEVEL_DRAFT;
      disableAggressiveOptimization = true;
    } else {
      optimize = Math.min(OPTIMIZE_LEVEL_DRAFT, Math.min(optimize, OPTIMIZE_LEVEL_MAX));
    }

    // TODO: staleness check (unless force==true)

    ClassWorld world = new ClassWorld();
    ClassRealm realm;
    try {
      realm = world.newRealm("gwt", null);
      for (String elt : project.getCompileSourceRoots()) {
        URL url = new File(elt).toURI().toURL();
        realm.addURL(url);
        if (getLog().isDebugEnabled()) {
          getLog().debug("Source root: " + url);
        }
      }
      for (String elt : project.getCompileClasspathElements()) {
        URL url = new File(elt).toURI().toURL();
        realm.addURL(url);
        if (getLog().isDebugEnabled()) {
          getLog().debug("Compile classpath: " + url);
        }
      }
      // TODO: only include gwt-dev
      for (Artifact pluginArtifact : pluginArtifacts) {
        URL url = pluginArtifact.getFile().toURI().toURL();
        realm.addURL(url);
        if (getLog().isDebugEnabled()) {
          getLog().debug("Plugin artifact: " + url);
        }
      }
    } catch (DuplicateRealmException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (MalformedURLException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    // Argument to Compiler ctor
    importFromCurrentClassLoader(realm, CompilerOptions.class);
    // Argument to Compiler#run
    importFromCurrentClassLoader(realm, TreeLogger.class);
    // Referenced by CompilerOptions; TreeLogger.Type is already imported via TreeLogger above
    importFromCurrentClassLoader(realm, JsOutputOption.class);
    // Makes error check easier
    importFromCurrentClassLoader(realm, UnableToCompleteException.class);

    Thread.currentThread().setContextClassLoader(realm);

    // Use only threads (no sub-process; ExternaPermutationWorkerFactory won't use the correct classpath)
    System.setProperty(PermutationWorkerFactory.FACTORY_IMPL_PROPERTY, ThreadedPermutationWorkerFactory.class.getName());
    // Use as many threads as available processors
    System.setProperty(ThreadedPermutationWorkerFactory.MAX_THREADS_PROPERTY, "" + Runtime.getRuntime().availableProcessors());

    boolean success;
    MavenTreeLogger logger = MavenTreeLogger.newInstance(getLog(), logLevel);
    try {
      Class<?> compilerClass = Class.forName(Compiler.class.getName(), true, realm);
      Constructor<?> ctor = compilerClass.getConstructor(CompilerOptions.class);
      Object compiler = ctor.newInstance(this);
      Method runMethod = compilerClass.getMethod("run", TreeLogger.class);
      success = (Boolean) runMethod.invoke(compiler, logger);

      // TODO: Copy thread-termination code from exec-maven-plugin; directly call PersistenUnitCache.shutdown() ?
    } catch (Throwable t) {
      if (t instanceof InvocationTargetException && t.getCause() instanceof UnableToCompleteException) {
        // assume logged
      } else {
        CompilationProblemReporter.logAndTranslateException(logger, t);
      }
      success = false;
    }

    if (!success) {
      throw new MojoExecutionException("GWT compilation failed");
    }
  }

  private void importFromCurrentClassLoader(ClassRealm realm, Class<?> cls) {
    if (cls == null || !cls.getName().startsWith("com.google.gwt.")) {
      return;
    }
    realm.importFrom(Thread.currentThread().getContextClassLoader(), cls.getName());
    // ClassRealm importing is prefix-based, so no need to specifically add inner classes
    for (Class<?> intf : cls.getInterfaces()) {
      importFromCurrentClassLoader(realm, intf);
    }
    importFromCurrentClassLoader(realm, cls.getSuperclass());
  }

  @Override
  public boolean isAggressivelyOptimize() {
    return !disableAggressiveOptimization;
  }

  @Override
  public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
    disableAggressiveOptimization = !aggressivelyOptimize;
  }

  @Override
  public boolean isCompilerMetricsEnabled() {
    return compilerMetrics;
  }

  @Override
  public void setCompilerMetricsEnabled(boolean enabled) {
    compilerMetrics = enabled;
  }

  @Override
  public File getDeployDir() {
    return deploy;
  }

  @Override
  public void setDeployDir(File dir) {
    deploy = dir;
  }

  @Override
  public boolean isCastCheckingDisabled() {
    return disableCastChecking;
  }

  @Override
  public void setCastCheckingDisabled(boolean disabled) {
    disableCastChecking = disabled;
  }

  @Override
  public boolean isClassMetadataDisabled() {
    return disableClassMetadata;
  }

  @Override
  public void setClassMetadataDisabled(boolean disabled) {
    disableClassMetadata = disabled;
  }

  @Override
  public boolean isUpdateCheckDisabled() {
    return disableUpdateCheck;
  }

  @Override
  public void setDisableUpdateCheck(boolean disabled) {
    disableUpdateCheck = disabled;
  }

  @Override
  public boolean isEnableAssertions() {
    return enableAssertions;
  }

  @Override
  public void setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
  }

  @Override
  public boolean isClosureCompilerEnabled() {
    return enableClosureCompiler;
  }

  @Override
  public void setClosureCompilerEnabled(boolean enabled) {
    enableClosureCompiler = enabled;
  }

  @Override
  public boolean isEnabledGeneratingOnShards() {
    return !disableGeneratingOnShards;
  }

  @Override
  public void setEnabledGeneratingOnShards(boolean allowed) {
    disableGeneratingOnShards = !allowed;
  }

  @Override
  public File getExtraDir() {
    return extra;
  }

  @Override
  public void setExtraDir(File extraDir) {
    extra = extraDir;
  }

  @Override
  public int getFragmentCount() {
    return fragmentCount;
  }

  @Override
  public void setFragmentCount(int numFragments) {
    fragmentCount = numFragments;
  }

  @Override
  public int getFragmentsMerge() {
    return -1;
  }

  @Override
  public void setFragmentsMerge(int numFragments) {
    throw new UnsupportedOperationException();
  }

  @Override
  public File getGenDir() {
    return gen;
  }

  @Override
  public void setGenDir(File dir) {
    gen = dir;
  }

  @Override
  public boolean isUseGuiLogger() {
    return false;
  }

  @Override
  public void setUseGuiLogger(boolean useGuiLogger) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getLocalWorkers() {
    return localWorkers;
  }

  @Override
  public void setLocalWorkers(int localWorkers) {
    this.localWorkers = localWorkers;
  }

  @Override
  public TreeLogger.Type getLogLevel() {
    return (logLevel == null) ? MavenTreeLogger.getLogLevel(getLog()) : logLevel;
  }

  @Override
  public void setLogLevel(TreeLogger.Type logLevel) {
    this.logLevel = logLevel;
  }

  @Override
  public int getMaxPermsPerPrecompile() {
    return -1;
  }

  @Override
  public void setMaxPermsPerPrecompile(int maxPerms) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getModuleNames() {
    return Collections.singletonList(moduleName);
  }

  @Override
  public void addModuleName(String moduleName) {
    // This method is called as a setter for the field to configure the mojo
    if (this.moduleName != null) {
      throw new IllegalStateException();
    }
    this.moduleName = moduleName;
  }

  @Override
  public void setModuleNames(List<String> moduleNames) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getOptimizationLevel() {
    return optimize;
  }

  @Override
  public void setOptimizationLevel(int level) {
    optimize = level;
  }

  @Override
  public boolean isOptimizePrecompile() {
    return optimizePrecompile;
  }

  @Override
  public void setOptimizePrecompile(boolean optimize) {
    optimizePrecompile = optimize;
  }

  @Override
  public File getOutDir() {
    return null;
  }

  @Override
  public void setOutDir(File outDir) {
    // no-op
    // TODO?
  }

  @Override
  public boolean isRunAsyncEnabled() {
    return !disableRunAsync;
  }

  @Override
  public void setRunAsyncEnabled(boolean enabled) {
    disableRunAsync = !enabled;
  }

  @Override
  public JsOutputOption getOutput() {
    return style;
  }

  @Override
  public void setOutput(JsOutputOption obfuscated) {
    style = obfuscated;
  }

  @Override
  public boolean isSoycExtra() {
    return compileReport == CompileReport.DETAILED || compileReport == CompileReport.DETAILED_HTML;
  }

  @Override
  public void setSoycExtra(boolean soycExtra) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSoycEnabled() {
    return compileReport != CompileReport.OFF;
  }

  @Override
  public boolean isSoycHtmlDisabled() {
    return compileReport != CompileReport.HTML && compileReport != CompileReport.DETAILED_HTML;
  }

  @Override
  public void setSoycEnabled(boolean enabled) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSoycHtmlDisabled(boolean disabled) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isStrict() {
    return strict;
  }

  @Override
  public void setStrict(boolean strict) {
    this.strict = strict;
  }

  @Override
  public boolean isValidateOnly() {
    return validateOnly;
  }

  @Override
  public void setValidateOnly(boolean validateOnly) {
    this.validateOnly = validateOnly;
  }

  @Override
  public File getWarDir() {
    return webappDirectory;
  }

  @Override
  public void setWarDir(File dir) {
    this.webappDirectory = dir;
  }

  @Override
  public File getWorkDir() {
    return workDir;
  }

  @Override
  public void setWorkDir(File dir) {
    workDir = dir;
  }
}
