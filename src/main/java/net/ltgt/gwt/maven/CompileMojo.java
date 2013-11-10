package net.ltgt.gwt.maven;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.PermutationWorkerFactory;
import com.google.gwt.dev.ThreadedPermutationWorkerFactory;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.util.arg.SourceLevel;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mojo(name = "compile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
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
   * Sets whether or not the compiler should cluster similar functions together in the output file
   * whose source code is very similar, to increase the efficiency of compression.
   */
  @Parameter(property = "gwt.clusterSimilarFunctions", defaultValue = "true")
  private boolean clusterSimilarFunctions;

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
   * Sets whether the compiler should not implicitly add "client" and "public" resource dependencies
   * when none are mentioned.
   */
  @Parameter(property = "gwt.enforceStrictResources", defaultValue = "false")
  private boolean enforceStrictResources;

  /**
   * Disables running generators on CompilePerms shards, even when it would be a likely speedup.
   */
  @Parameter(property = "gwt.disableGeneratingOnShards", defaultValue = "false")
  private boolean disableGeneratingOnShards;

  /**
   * Whether or not to output extra files.
   */
  @Parameter(property = "gwt.outputExtra", defaultValue = "false")
  private boolean outputExtra;

  /**
   * The directory into which extra files, not intended for deployment, will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/extra")
  private File extra;

  /**
   * EXPERIMENTAL: Limits of number of fragments using a code splitter that merges split points.
   */
  @Parameter(property = "gwt.fragmentCount", defaultValue = "-1")
  private int fragmentCount;

  /**
   * Whether or not to output transient generated files.
   */
  @Parameter(property = "gwt.outputGen", defaultValue = "false")
  private boolean outputGen;

  /**
   * Debugging: causes normally-transient generated types to be saved in the specified directory.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/gen")
  private File gen;

  /**
   * Sets whether or not the compiler should inline literal parameters.
   */
  @Parameter(property = "gwt.inlineLiteralParameters", defaultValue = "true")
  private boolean inlineLiteralParameters;
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
   * The short name of the module, used to name the output {@code .nocache.js} file.
   */
  @Parameter
  private String moduleShortName;

  /**
   * Sets the optimization level used by the compiler.  0=none 9=maximum.
   */
  @Parameter(property = "gwt.optimize", defaultValue = "" + OPTIMIZE_LEVEL_MAX)
  private int optimize;

  /**
   * Sets whether or not the compiler should optimize dataflow.
   */
  @Parameter(property = "gwt.optimize.dataflow", defaultValue = "true")
  private boolean optimizeDataflow;

  /** This is set and read by the compiler. */
  private boolean optimizePrecompile = true;

  /**
   * Sets whether or not the compiler should ordinalize enums.
   */
  @Parameter(property = "gwt.ordinalizeEnums", defaultValue = "true")
  private boolean ordinalizeEnums;

  /**
   * Disable runAsync code-splitting.
   */
  @Parameter(property = "gwt.disableRunAsync", defaultValue = "false")
  private boolean disableRunAsync;

  /**
   * Sets whether or not the compiler should remove duplicate functions.
   */
  @Parameter(property = "gwt.removeDuplicateFunctions", defaultValue = "true")
  private boolean removeDuplicateFunctions;

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
   * If set to true and sourcemaps are on, source code will be saved.
   *
   * <p>By default, source code will be written to "extras". The {@code saveSourceOutput}
   * option can be used to redirect it to a different directory or jar.</p>
   */
  @Parameter(property = "gwt.saveSource", defaultValue = "false")
  private boolean saveSource;

  /**
   * Sets the directory or jar where the GWT compiler should write the source code,
   * or null to skip writing it. If a jar exists then it will be overwritten. By
   * default, source code will be written to "extras".
   *
   * <p>The debugger will find the source code using a sourcemap. To convert a filename
   * in a sourcemap to a filename in the debug source directory, add "${module_name}/src/"
   * to the front. (The module name is after any renaming.)</p>
   */
  @Parameter(property = "gwt.saveSourceOutput")
  private File saveSourceOutput;

  /**
   * Indicates the Java source level compatibility. Valid values include 1.6 and 1.7
   */
  @Parameter(property = "gwt.sourceLevel", defaultValue = "1.6")
  private String sourceLevel;
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

  // TODO: speedtracer

  @Component
  private MavenProject project;

  @Parameter(defaultValue = "${plugin.artifactMap}", required = true, readonly = true)
  private Map<String, Artifact> pluginArtifactMap;

  @Component
  private RepositorySystem repositorySystem;

  @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
  private ArtifactRepository localRepository;

  private Set<Artifact> gwtSdkArtifacts;

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

    if (!forceCompilation && !isStale()) {
      getLog().info("Compilation output seems uptodate. GWT compilation skipped.");
      return;
    }

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
      // gwt-dev and its transitive dependencies
      for (Artifact elt : getGwtDevArtifacts()) {
        URL url = elt.getFile().toURI().toURL();
        realm.addURL(url);
        if (getLog().isDebugEnabled()) {
          getLog().debug("Compile classpath: " + url);
        }
      }
      realm.addURL(pluginArtifactMap.get("com.google.gwt:gwt-dev").getFile().toURI().toURL());
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

    importFromCurrentClassLoader(realm, SourceLevel.class);

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

  private Set<Artifact> getGwtDevArtifacts() {
    if (gwtSdkArtifacts == null) {
      ArtifactResolutionRequest request = new ArtifactResolutionRequest()
          .setArtifact(pluginArtifactMap.get("com.google.gwt:gwt-dev"))
          .setResolveTransitively(true)
          .setLocalRepository(localRepository);
      ArtifactResolutionResult result = repositorySystem.resolve(request);
      gwtSdkArtifacts = result.getArtifacts();
    }
    return gwtSdkArtifacts;
  }

  private boolean isStale() throws MojoExecutionException {
    if (!webappDirectory.exists()) {
      return true;
    }

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

    ArrayList<String> sourceRoots = new ArrayList<String>();
    // sources (incl. generated sources)
    sourceRoots.addAll(project.getCompileSourceRoots());
    // compiled (processed) classes and resources (incl. processed and generated ones)
    sourceRoots.add(project.getBuild().getOutputDirectory());
    for (String sourceRoot : sourceRoots) {
      File rootFile = new File( sourceRoot );

      if (isStale(scanner, rootFile, nocacheJs)) {
        return true;
      }
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
    // gwt-dev and its transitive dependencies
    for (Artifact artifact : getGwtDevArtifacts()) {
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
  public boolean shouldClusterSimilarFunctions() {
    return clusterSimilarFunctions;
  }

  @Override
  public void setClusterSimilarFunctions(boolean enabled) {
    clusterSimilarFunctions = enabled;
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
  public boolean enforceStrictResources() {
    return enforceStrictResources;
  }

  @Override
  public void setEnforceStrictResources(boolean strictResources) {
    enforceStrictResources = strictResources;
  }

  @Override
  public File getExtraDir() {
    return outputExtra ? extra : null;
  }

  @Override
  public void setExtraDir(File extraDir) {
    this.extra = extraDir;
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
    return outputGen ? gen : null;
  }

  @Override
  public void setGenDir(File dir) {
    gen = dir;
  }

  @Override
  public boolean shouldInlineLiteralParameters() {
    return inlineLiteralParameters;
  }

  @Override
  public void setInlineLiteralParameters(boolean enabled) {
    inlineLiteralParameters = enabled;
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
  public boolean shouldOptimizeDataflow() {
    return optimizeDataflow;
  }

  @Override
  public void setOptimizeDataflow(boolean enabled) {
    optimizeDataflow = enabled;
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
  public boolean shouldOrdinalizeEnums() {
    return ordinalizeEnums;
  }

  @Override
  public void setOrdinalizeEnums(boolean enabled) {
    ordinalizeEnums = enabled;
  }

  @Override
  public boolean shouldRemoveDuplicateFunctions() {
    return removeDuplicateFunctions;
  }

  @Override
  public void setRemoveDuplicateFunctions(boolean enabled) {
    removeDuplicateFunctions = enabled;
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
  public boolean shouldSaveSource() {
    return saveSource;
  }

  @Override
  public void setSaveSource(boolean enabled) {
    saveSource = enabled;
  }

  @Override
  public File getSaveSourceOutput() {
    return saveSourceOutput;
  }

  @Override
  public void setSaveSourceOutput(File dest) {
    saveSourceOutput = dest;
  }

  @Override
  public SourceLevel getSourceLevel() {
    return SourceLevel.fromString(sourceLevel);
  }

  @Override
  public void setSourceLevel(SourceLevel level) {
    sourceLevel = level.getStringValue();
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
