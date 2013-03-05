package net.ltgt.gwt.maven;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.JsOutputOption;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.plugin.surefire.booterclient.ForkConfiguration;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.util.NestedCheckedException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class TestMojo extends AbstractSurefireMojo implements SurefireReportParameters {

  // GWT-specific properties
  /**
   * Specifies the TCP port for the embedded web server (defaults to automatically picking an available port)
   */
  @Parameter(property = "gwt.port")
  private int port;

  // whitelist/blacklist?
  // TODO(t.broyer): log to file?

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

  private TreeLogger.Type getLogLevel() {
    return (logLevel == null) ? MavenTreeLogger.getLogLevel(getLog()) : logLevel;
  }

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

  private File getGenDir() {
    return outputGen ? gen : null;
  }

  /**
   * Specifies the TCP port for the code server (defaults to automatically picking an available port)
   */
  @Parameter(property = "gwt.codeServerPort")
  private int codeServerPort;

  /**
   * The directory into which deployable but not servable output files will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/extra")
  private File deploy;

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

  private File getExtraDir() {
    return outputExtra ? extra : null;
  }

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/work")
  private File workDir;

  /**
   * Script output style: OBFUSCATED, PRETTY, or DETAILED.
   */
  @Parameter(property = "gwt.style", defaultValue = "OBFUSCATED")
  private JsOutputOption style;

  /**
   * Troubleshooting: Prevent the Production Mode compiler from performing aggressive optimizations.
   */
  @Parameter(property = "gwt.disableAggressiveOptimization", defaultValue = "false")
  private boolean disableAggressiveOptimization;

  /**
   * EXPERIMENTAL: Disables some {@code java.lang.Class} methods (e.g. {@code getName()}).
   */
  @Parameter(property = "gwt.disableClassMetadata", defaultValue = "false")
  private boolean disableClassMetadata;

  /**
   * EXPERIMENTAL: Disables run-time checking of cast operations.
   */
  @Parameter(property = "gwt.disableCastChecking", defaultValue = "false")
  private boolean disableCastChecking;

  /**
   * Disable runAsync code-splitting.
   */
  @Parameter(property = "gwt.disableRunAsync", defaultValue = "false")
  private boolean disableRunAsync;

  /**
   * Disable the check to see if an update version of GWT is available.
   */
  @Parameter(property = "gwt.disableUpdateCheck", defaultValue = "false")
  private boolean disableUpdateCheck;

  /**
   * Enable faster, but less-optimized, compilations.
   */
  @Parameter(property = "gwt.draftCompile", defaultValue = "false")
  private boolean draftCompile;

  /**
   * The number of local workers to use when compiling permutations.
   */
  @Parameter(property = "gwt.localWorkers")
  private int localWorkers;

  /**
   * Causes your test to run in production (compiled) mode (defaults to development mode)
   */
  @Parameter(property = "gwt.test.prod")
  private boolean prod;

  /**
   * Set the test method timeout, in minutes.
   */
  @Parameter(property = "gwt.test.methodTimeout", defaultValue = "5")
  private int testMethodTimeout;

  /**
   * Set the test begin timeout (time for clients to contact server), in minutes.
   */
  @Parameter(property = "gwt.test.beginTimeout", defaultValue = "1")
  private int testBeginTimeout;

  /**
   * Selects the runstyle to use for this test.  The name is a suffix of
   * {@code com.google.gwt.junit.RunStyle} or is a fully qualified class name, and may be 
   * followed with a colon and an argument for this runstyle.  The specified class must
   * extend RunStyle.
   */
  @Parameter(property = "gwt.test.runStyle", defaultValue = "HtmlUnit")
  private String runStyle;

  /**
   * Configure batch execution of tests. Value can be one of none, class or module.
   */
  @Parameter(property = "gwt.test.batch", defaultValue = "none")
  private String batch;

  public void setBatch(String batch) {
    if (!batch.equals("none") && !batch.equals("class") && !batch.equals("module")) {
      throw new IllegalArgumentException("batch");
    }
    this.batch = batch;
  }

  /**
   * Causes the log window and browser windows to be displayed; useful for debugging.
   */
  @Parameter(property = "gwt.test.notHeadless")
  private boolean notHeadless;

  /**
   * Precompile modules as tests are running (speeds up remote tests but requires more memory).
   * Value can be one of simple, all or parallel.
   */
  @Parameter(property = "gwt.test.precompile", defaultValue = "simple")
  private String precompile;

  public void setPrecompile(String precompile) {
    if (!precompile.equals("simple") && !precompile.equals("all") && !precompile.equals("parallel")) {
      throw new IllegalArgumentException("precompile");
    }
    this.precompile = precompile;
  }

  /**
   * Run each test using an HTML document in quirks mode (rather than standards mode).
   */
  @Parameter(property = "gwt.test.quirksMode")
  private boolean quirksMode;

  /**
   * EXPERIMENTAL: Sets the maximum number of attempts for running each test method.
   */
  @Parameter(property = "gwt.test.tries", defaultValue = "1")
  private int tries;

  /**
   * Specify the user agents to reduce the number of permutations for remote browser tests;
   * e.g. ie6,ie8,safari,gecko1_8,opera.
   */
  @Parameter(property = "gwt.test.userAgents")
  private String userAgents;

  /**
   * The directory to write output files into.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt-tests/www")
  private File outDir;

  @Override
  public Map<String, String> getSystemPropertyVariables() {
    Map<String, String> props = super.getSystemPropertyVariables();
    if (props == null) {
      props = new HashMap<String, String>(2);
    }
    if (!props.containsKey("gwt.args")) {
      StringBuilder sb = new StringBuilder();
      if (port > 0) {
        sb.append(" -port ").append(port);
      } else {
        sb.append(" -port auto ");
      }
      sb.append(" -logLevel ").append(getLogLevel());
      File gen = getGenDir();
      if (gen != null) {
        sb.append(" -gen ").append(quote(gen.getAbsolutePath()));
      }
      if (codeServerPort > 0) {
        sb.append(" -codeServerPort ").append(codeServerPort);
      } else {
        sb.append(" -codeServerPort auto ");
      }
      sb.append(" -deploy ").append(quote(deploy.getAbsolutePath()));
      File extra = getExtraDir();
      if (extra != null) {
        sb.append(" -extra ").append(quote(extra.getAbsolutePath()));
      }
      sb.append(" -workDir ").append(quote(workDir.getAbsolutePath()));
      sb.append(" -style ").append(style);
      if (effectiveIsEnableAssertions()) {
        sb.append(" -ea ");
      }
      if (disableAggressiveOptimization) {
        sb.append(" -XdisableAggressiveOptimization ");
      }
      if (disableClassMetadata) {
        sb.append(" -XdisableClassMetadata ");
      }
      if (disableCastChecking) {
        sb.append(" -XdisableCastChecking ");
      }
      if (disableRunAsync) {
        sb.append(" -XdisableRunAsync ");
      }
      if (disableUpdateCheck) {
        sb.append(" -XdisableUpdateCheck ");
      }
      if (draftCompile) {
        sb.append(" -draftCompile ");
      }
      int workers = localWorkers;
      if (workers < 1) {
        workers = Runtime.getRuntime().availableProcessors();
        if (getLog().isDebugEnabled()) {
          getLog().debug("Using " + workers + " local workers");
        }
      }
      sb.append(" -localWorkers ").append(workers);
      if (prod) {
        sb.append(" -prod ");
      }
      sb.append(" -testMethodTimeout ").append(testMethodTimeout);
      sb.append(" -testBeginTimeout ").append(testBeginTimeout);
      sb.append(" -runStyle ").append(quote(runStyle));
      sb.append(" -batch ").append(batch);
      if (notHeadless) {
        sb.append(" -notHeadless ");
      }
      sb.append(" -precompile ").append(precompile);
      if (quirksMode) {
        sb.append(" -quirksMode ");
      }
      sb.append(" -Xtries ").append(tries);
      if (StringUtils.isNotBlank(userAgents)) {
        sb.append(" -userAgents ").append(quote(userAgents));
      }
      sb.append(" -war ").append(quote(outDir.getAbsolutePath()));

      props.put("gwt.args", sb.toString());
    }
    if (getLog().isDebugEnabled()) {
      getLog().debug("Using gwt.args: " + props.get("gwt.args"));
    }
    return props;
  }

  private Object quote(String value) {
    if (value.matches(".*[\"\\s].*")) {
      return "\"" + value.replace("\"", "\\\"") + "\"";
    }
    return value;
  }

  @Override
  public void setSystemPropertyVariables(Map<String, String> systemPropertyVariables) {
    if (systemPropertyVariables.containsKey("gwt.args")) {
      getLog().warn("systemPropertyVariables contains a gwt.args value, this will override all individual options");
    }
    super.setSystemPropertyVariables(systemPropertyVariables);
  }

  @Override
  protected void addPluginSpecificChecksumItems(ChecksumCalculator checksum) {
    checksum.add(port);
    checksum.add(String.valueOf(logLevel));
    checksum.add(outputGen);
    checksum.add(gen);
    checksum.add(codeServerPort);
    checksum.add(deploy);
    checksum.add(outputExtra);
    checksum.add(extra);
    checksum.add(workDir);
    checksum.add(String.valueOf(style));
    checksum.add(disableAggressiveOptimization);
    checksum.add(disableClassMetadata);
    checksum.add(disableCastChecking);
    checksum.add(disableRunAsync);
    checksum.add(disableUpdateCheck);
    checksum.add(draftCompile);
    checksum.add(localWorkers);
    checksum.add(prod);
    checksum.add(testBeginTimeout);
    checksum.add(testMethodTimeout);
    checksum.add(runStyle);
    checksum.add(batch);
    checksum.add(notHeadless);
    checksum.add(precompile);
    checksum.add(quirksMode);
    checksum.add(tries);
    checksum.add(userAgents);
  }

  @Component
  private RepositorySystem repositorySystem;

  private Set<String> gwtDevArtifacts;

  @Override
  public List<String> getAdditionalClasspathElements() {
    LinkedHashSet<String> elts = new LinkedHashSet<String>();
    if (additionalClasspathElements != null) {
      elts.addAll(additionalClasspathElements);
    }
    if (gwtDevArtifacts == null) {
      ArtifactResolutionRequest request = new ArtifactResolutionRequest()
          .setArtifact(pluginArtifactMap.get("com.google.gwt:gwt-dev"))
          .setResolveTransitively(true)
          .setLocalRepository(localRepository)
          .setRemoteRepositories(remoteRepositories);
      ArtifactResolutionResult result = repositorySystem.resolve(request);
      gwtDevArtifacts = new LinkedHashSet<String>();
      for (Artifact artifact : result.getArtifacts()) {
        gwtDevArtifacts.add(artifact.getFile().getAbsolutePath());
      }
    }
    elts.addAll(gwtDevArtifacts);
    return new ArrayList<String>(elts);
  }

  // Properties copied from Surefire

  /**
   * Set this to "true" to ignore a failure during testing. Its use is NOT
   * RECOMMENDED, but quite convenient on occasion.
   */
  @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
  private boolean testFailureIgnore;

  /**
   * Base directory where all reports are written to.
   */
  @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
  private File reportsDirectory;

  /**
   * Specify this parameter to run individual tests by file name, overriding the
   * <code>includes/excludes</code> parameters. Each pattern you specify here
   * will be used to create an include pattern formatted like
   * <code>**&#47;${test}.java</code>, so you can just type "-Dtest=MyTest" to
   * run a single test called "foo/MyTest.java".<br/>
   * This parameter overrides the <code>includes/excludes</code> parameters, and
   * the TestNG <code>suiteXmlFiles</code> parameter.
   * <p/>
   * Since 2.7.3, you can execute a limited number of methods in the test by
   * adding #myMethod or #my*ethod. For example, "-Dtest=MyTest#myMethod". This
   * is supported for junit 4.x and testNg.
   */
  @Parameter(property = "test")
  private String test;

  /**
   * Option to print summary of test suites or just print the test cases that
   * have errors.
   */
  @Parameter(property = "surefire.printSummary", defaultValue = "true")
  private boolean printSummary;

  /**
   * Selects the formatting for the test report to be generated. Can be set as
   * "brief" or "plain". Only applies to the output format of the output files
   * (target/surefire-reports/testName.txt)
   */
  @Parameter(property = "surefire.reportFormat", defaultValue = "brief")
  private String reportFormat;

  /**
   * Option to generate a file test report or just output the test report to the
   * console.
   */
  @Parameter(property = "surefire.useFile", defaultValue = "true")
  private boolean useFile;

  /**
   * Set this to "true" to cause a failure if the none of the tests specified in
   * -Dtest=... are run. Defaults to "true".
   * 
   * @since 2.12
   */
  @Parameter(property = "surefire.failIfNoSpecifiedTests")
  private Boolean failIfNoSpecifiedTests;

  /**
   * Attach a debugger to the forked JVM. If set to "true", the process will
   * suspend and wait for a debugger to attach on port 5005. If set to some
   * other string, that string will be appended to the argLine, allowing you to
   * configure arbitrary debuggability options (without overwriting the other
   * options specified through the <code>argLine</code> parameter).
   * 
   * @since 2.4
   */
  @Parameter(property = "maven.surefire.debug")
  private String debugForkedProcess;

  /**
   * Kill the forked test process after a certain number of seconds. If set to
   * 0, wait forever for the process, never timing out.
   * 
   * @since 2.4
   */
  @Parameter(property = "surefire.timeout")
  private int forkedProcessTimeoutInSeconds;

  @Override
  protected void handleSummary(RunResult summary, NestedCheckedException firstForkException)
      throws MojoExecutionException, MojoFailureException {
    assertNoException(firstForkException);

    SurefireHelper.reportExecution(this, summary, getLog());
  }

  private void assertNoException(NestedCheckedException firstForkException)
      throws MojoFailureException {
    if (firstForkException != null) {
      throw new MojoFailureException(firstForkException.getMessage(), firstForkException);
    }
  }

  @Override
  protected boolean isSkipExecution() {
    return isSkip() || isSkipTests() || isSkipExec();
  }

  @Override
  protected String getPluginName() {
    return "GWT tests";
  }

  @Override
  protected String[] getDefaultIncludes() {
    return new String[] {"**/*Suite.java", "**/*SuiteNoBrowser.java"};
  }

  @Override
  public void setForkMode(String forkMode) {
    if (ForkConfiguration.FORK_NEVER.equals(forkMode)) {
      getLog().warn("ForkMode=never is know not to work for GWT tests");
    }
    super.setForkMode(forkMode);
  }

  //

  @Override
  public boolean isSkipTests() {
    return skipTests;
  }

  @Override
  public void setSkipTests(boolean skipTests) {
    this.skipTests = skipTests;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isSkipExec() {
    return skipExec;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void setSkipExec(boolean skipExec) {
    this.skipExec = skipExec;
  }

  @Override
  public boolean isSkip() {
    return skip;
  }

  @Override
  public void setSkip(boolean skip) {
    this.skip = skip;
  }

  @Override
  public File getBasedir() {
    return basedir;
  }

  @Override
  public void setBasedir(File basedir) {
    this.basedir = basedir;
  }

  @Override
  public File getTestClassesDirectory() {
    return testClassesDirectory;
  }

  @Override
  public void setTestClassesDirectory(File testClassesDirectory) {
    this.testClassesDirectory = testClassesDirectory;
  }

  @Override
  public File getClassesDirectory() {
    return classesDirectory;
  }

  @Override
  public void setClassesDirectory(File classesDirectory) {
    this.classesDirectory = classesDirectory;
  }

  @Override
  public List<String> getClasspathDependencyExcludes() {
    return classpathDependencyExcludes;
  }

  @Override
  public void setClasspathDependencyExcludes(List<String> classpathDependencyExcludes) {
    this.classpathDependencyExcludes = classpathDependencyExcludes;
  }

  @Override
  public String getClasspathDependencyScopeExclude() {
    return classpathDependencyScopeExclude;
  }

  @Override
  public void setClasspathDependencyScopeExclude(String classpathDependencyScopeExclude) {
    this.classpathDependencyScopeExclude = classpathDependencyScopeExclude;
  }

  // getAdditionalClasspathElements is defined above

  @Override
  public void setAdditionalClasspathElements(List<String> additionalClasspathElements) {
    this.additionalClasspathElements = additionalClasspathElements;
  }

  @Override
  public File getReportsDirectory() {
    return reportsDirectory;
  }

  @Override
  public void setReportsDirectory(File reportsDirectory) {
    this.reportsDirectory = reportsDirectory;
  }

  @Override
  public String getTest() {
    if (StringUtils.isBlank(test)) {
      return null;
    }
    String[] testArray = StringUtils.split(test, ",");
    StringBuilder tests = new StringBuilder();
    for (String aTestArray : testArray) {
      String singleTest = aTestArray;
      int index = singleTest.indexOf('#');
      if (index >= 0) {// single test method
        singleTest = singleTest.substring(0, index);
      }
      tests.append(singleTest);
      tests.append(",");
    }
    return tests.toString();
  }

  @Override
  public String getTestMethod() {
    if (StringUtils.isBlank(test)) {
      return null;
    }
    // modified by rainLee, see http://jira.codehaus.org/browse/SUREFIRE-745
    int index = this.test.indexOf('#');
    int index2 = this.test.indexOf(",");
    if (index >= 0) {
      if (index2 < 0) {
        String testStrAfterFirstSharp = this.test.substring(index + 1, this.test.length());
        if (!testStrAfterFirstSharp.contains("+")) {// the original way
          return testStrAfterFirstSharp;
        } else {
          return this.test;
        }
      } else {
        return this.test;
      }
    }
    return null;
  }

  @Override
  public void setTest(String test) {
    this.test = test;
  }

  @Override
  public boolean isPrintSummary() {
    return printSummary;
  }

  @Override
  public void setPrintSummary(boolean printSummary) {
    this.printSummary = printSummary;
  }

  @Override
  public String getReportFormat() {
    return reportFormat;
  }

  @Override
  public void setReportFormat(String reportFormat) {
    this.reportFormat = reportFormat;
  }

  @Override
  public boolean isUseFile() {
    return useFile;
  }

  @Override
  public void setUseFile(boolean useFile) {
    this.useFile = useFile;
  }

  @Override
  public String getDebugForkedProcess() {
    return debugForkedProcess;
  }

  @Override
  public void setDebugForkedProcess(String debugForkedProcess) {
    this.debugForkedProcess = debugForkedProcess;
  }

  @Override
  public int getForkedProcessTimeoutInSeconds() {
    return forkedProcessTimeoutInSeconds;
  }

  @Override
  public void setForkedProcessTimeoutInSeconds(int forkedProcessTimeoutInSeconds) {
    this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
  }

  @Override
  public boolean isUseSystemClassLoader() {
    // GWTTestCase must use system class-loader
    return true;
  }

  @Override
  public void setUseSystemClassLoader(boolean useSystemClassLoader) {
    throw new UnsupportedOperationException("useSystemClassLoader is read-only");
  }

  @Override
  public boolean isUseManifestOnlyJar() {
    // GWTTestCase must not use manifest-only JAR
    return false;
  }

  @Override
  public void setUseManifestOnlyJar(boolean useManifestOnlyJar) {
    throw new UnsupportedOperationException("useManifestOnlyJar is read-only");
  }

  @Override
  public Boolean getFailIfNoSpecifiedTests() {
    return failIfNoSpecifiedTests;
  }

  @Override
  public void setFailIfNoSpecifiedTests(Boolean failIfNoSpecifiedTests) {
    this.failIfNoSpecifiedTests = failIfNoSpecifiedTests;
  }

  @Override
  public boolean isTestFailureIgnore() {
    return testFailureIgnore;
  }

  @Override
  public void setTestFailureIgnore(boolean testFailureIgnore) {
    this.testFailureIgnore = testFailureIgnore;
  }
}
