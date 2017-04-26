package net.ltgt.gwt.maven;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.plugin.surefire.booterclient.ChecksumCalculator;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.suite.RunResult;

/**
 * Runs the project's tests with the specific setup needed for {@code GWTTestCase} tests.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class TestMojo extends AbstractSurefireMojo implements SurefireReportParameters, GwtOptions {

  // GWT-specific properties
  /**
   * Specifies the TCP port for the embedded web server (defaults to automatically picking an available port)
   */
  @Parameter(property = "gwt.port")
  private int port;

  /**
   * Specifies the TCP port for the code server (defaults to automatically picking an available port)
   */
  @Parameter(property = "gwt.codeServerPort")
  private int codeServerPort;

  /**
   * The directory into which deployable but not servable output files will be written.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt-tests/deploy")
  private File deploy;

  /**
   * The directory into which extra files, not intended for deployment, will be written.
   */
  @Parameter
  private File extra;

  /**
   * The compiler work directory (must be writeable).
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt/work")
  private File workDir;

  /**
   * Script output style: OBFUSCATED, PRETTY, or DETAILED.
   */
  @Parameter(property = "gwt.style")
  private String style;

  /**
   * Compile quickly with minimal optimizations.
   */
  @Parameter(property = "gwt.draftCompile", defaultValue = "false")
  private boolean draftCompile;

  /**
   * The number of local workers to use when compiling permutations. When terminated
   * with "C", the number part is multiplied with the number of CPU cores. Floating
   * point values are only accepted together with "C".
   */
  @Parameter(property = "gwt.localWorkers")
  private String localWorkers;

  /**
   * Sets the level of logging detail.
   */
  @Parameter(property = "gwt.logLevel")
  private String logLevel;

  /**
   * Sets the optimization level used by the compiler.  0=none 9=maximum.
   */
  @Parameter(property = "gwt.optimize")
  private Integer optimize;

  /**
   * Specifies Java source level.
   */
  @Parameter(property = "maven.compiler.source")
  private String sourceLevel;

  /**
   * The directory to write output files into.
   */
  @Parameter(defaultValue = "${project.build.directory}/gwt-tests/www")
  private File outDir;

  /**
   * Additional arguments to be passed to the GWT compiler.
   */
  @Parameter
  private List<String> compilerArgs;

  /**
   * Additional arguments to be passed to the JUnitShell.
   */
  @Parameter
  private List<String> testArgs;

  /**
   * Whether to prepend {@code #compilerArgs} to {@link #testArgs}.
   * <p>
   * This allows reuse when the {@code #compilerArgs} aren't incompatible with JUnitShell.
   */
  @Parameter(defaultValue = "false")
  private boolean useCompilerArgsForTests;

  @Override
  public Map<String, String> getSystemPropertyVariables() {
    Map<String, String> props = super.getSystemPropertyVariables();
    if (props == null) {
      props = new HashMap<>(2);
    }
    if (!props.containsKey("gwt.args")) {
      StringBuilder sb = new StringBuilder();
      if (port > 0) {
        sb.append(" -port ").append(port);
      }
      if (codeServerPort > 0) {
        sb.append(" -codeServerPort ").append(codeServerPort);
      }
      for (String arg : CommandlineBuilder.buildArgs(getLog(), this)) {
        sb.append(" ").append(quote(arg));
      }
      sb.append(effectiveIsEnableAssertions() ? " -checkAssertions" : " -nocheckAssertions");

      if (useCompilerArgsForTests && compilerArgs != null) {
        for (String arg : compilerArgs) {
          sb.append(" ").append(quote(arg));
        }
      }
      if (testArgs != null) {
        for (String arg : testArgs) {
          sb.append(" ").append(quote(arg));
        }
      }

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
    checksum.add(codeServerPort);
    checksum.add(deploy);
    checksum.add(extra);
    checksum.add(workDir);
    checksum.add(style);
    checksum.add(draftCompile);
    checksum.add(localWorkers);
    checksum.add(sourceLevel);
    checksum.add(testArgs);
    checksum.add(useCompilerArgsForTests);
    if (useCompilerArgsForTests) {
      checksum.add(compilerArgs);
    }
  }

  private String[] computedAdditionalClasspathElements;

  @Override
  public String[] getAdditionalClasspathElements() {
    if (computedAdditionalClasspathElements == null) {
      List<Resource> resources = new ArrayList<>();
      resources.addAll(getProject().getResources());
      resources.addAll(getProject().getTestResources());
      List<String> sourceRoots = new ArrayList<>();
      sourceRoots.addAll(getProject().getCompileSourceRoots());
      sourceRoots.addAll(getProject().getTestCompileSourceRoots());

      List<String> filteredSourceRoots = SourcesAsResourcesHelper.filterSourceRoots(getLog(), resources, sourceRoots);

      filteredSourceRoots.addAll(Arrays.asList(super.getAdditionalClasspathElements()));

      computedAdditionalClasspathElements = sourceRoots.toArray(new String[filteredSourceRoots.size()]);
    }
    return computedAdditionalClasspathElements;
  }

  // Properties copied from Surefire

  /**
   * The directory containing generated classes of the project being tested. This will be included after the test
   * classes in the test classpath.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}")
  private File classesDirectory;

  /**
   * Set this to "true" to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
   * occasion.
   */
  @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
  private boolean testFailureIgnore;

  /**
   * Base directory where all reports are written to.
   */
  @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
  private File reportsDirectory;

  /**
   * Specify this parameter to run individual tests by file name, overriding the <code>includes/excludes</code>
   * parameters. Each pattern you specify here will be used to create an include pattern formatted like
   * <code>**&#47;${test}.java</code>, so you can just type "-Dtest=MyTest" to run a single test called
   * "foo/MyTest.java". The test patterns prefixed with a <code>!</code> will be excluded.<br/>
   * This parameter overrides the <code>includes/excludes</code> parameters, and the TestNG <code>suiteXmlFiles</code>
   * parameter.
   * <p/>
   * Since 2.7.3, you can execute a limited number of methods in the test by adding #myMethod or #my*ethod. For
   * example, "-Dtest=MyTest#myMethod". This is supported for junit 4.x and testNg.<br/>
   * <br/>
   * Since 2.19 a complex syntax is supported in one parameter (JUnit 4, JUnit 4.7+, TestNG):<br/>
   * "-Dtest=???Test, !Unstable*, pkg&#47;**&#47;Ci*leTest.java, *Test#test*One+testTwo?????, #fast*+slowTest"<br/>
   * "-Dtest=Basic*, !%regex[.*.Unstable.*], !%regex[.*.MyTest.class#one.*|two.*], %regex[#fast.*|slow.*]"<br/>
   * <br/>
   * The Parameterized JUnit runner <em>describes</em> test methods using an index in brackets, so the non-regex
   * method pattern would become: <em>#testMethod[*]</em>. If using <em>@Parameters(name="{index}: fib({0})={1}")</em>
   * and selecting the index e.g. 5 in pattern, the non-regex method pattern would become <em>#testMethod[5:*]</em>.
   * <br/>
   */
  @Parameter(property = "test")
  private String test;

  /**
   * Option to print summary of test suites or just print the test cases that have errors.
   */
  @Parameter(property = "surefire.printSummary", defaultValue = "true")
  private boolean printSummary;

  /**
   * Selects the formatting for the test report to be generated. Can be set as "brief" or "plain".
   * Only applies to the output format of the output files  (target/surefire-reports/testName.txt)
   */
  @Parameter(property = "surefire.reportFormat", defaultValue = "brief")
  private String reportFormat;

  /**
   * Option to generate a file test report or just output the test report to the console.
   */
  @Parameter(property = "surefire.useFile", defaultValue = "true")
  private boolean useFile;

  /**
   * Set this to "true" to cause a failure if the none of the tests specified in -Dtest=... are run. Defaults to
   * "true".
   */
  @Parameter(property = "surefire.failIfNoSpecifiedTests")
  private Boolean failIfNoSpecifiedTests;

  /**
   * Attach a debugger to the forked JVM. If set to "true", the process will suspend and wait for a debugger to attach
   * on port 5005. If set to some other string, that string will be appended to the argLine, allowing you to configure
   * arbitrary debuggability options (without overwriting the other options specified through the <code>argLine</code>
   * parameter).
   */
  @Parameter(property = "maven.surefire.debug")
  private String debugForkedProcess;

  /**
   * Kill the forked test process after a certain number of seconds. If set to 0, wait forever for the process, never
   * timing out.
   */
  @Parameter(property = "surefire.timeout")
  private int forkedProcessTimeoutInSeconds;

  /**
   * Forked process is normally terminated without any significant delay after given tests have completed.
   * If the particular tests started non-daemon Thread(s), the process hangs instead of been properly terminated
   * by <em>System.exit()</em>. Use this parameter in order to determine the timeout of terminating the process.
   * <a href="http://maven.apache.org/surefire/maven-surefire-plugin/examples/shutdown.html">see the documentation:
   * http://maven.apache.org/surefire/maven-surefire-plugin/examples/shutdown.html</a>
   * Turns to default fallback value of 30 seconds if negative integer.
   */
  @Parameter(property = "surefire.exitTimeout", defaultValue = "30")
  private int forkedProcessExitTimeoutInSeconds;

  /**
   * Stop executing queued parallel JUnit tests after a certain number of seconds.
   * <br/>
   * Example values: "3.5", "4"<br/>
   * <br/>
   * If set to 0, wait forever, never timing out.
   * Makes sense with specified <code>parallel</code> different from "none".
   */
  @Parameter(property = "surefire.parallel.timeout")
  private double parallelTestsTimeoutInSeconds;

  /**
   * Stop executing queued parallel JUnit tests
   * and <em>interrupt</em> currently running tests after a certain number of seconds.
   * <br/>
   * Example values: "3.5", "4"<br/>
   * <br/>
   * If set to 0, wait forever, never timing out.
   * Makes sense with specified <code>parallel</code> different from "none".
   */
  @Parameter(property = "surefire.parallel.forcedTimeout")
  private double parallelTestsTimeoutForcedInSeconds;

  /**
   * A list of &lt;include> elements specifying the tests (by pattern) that should be included in testing. When not
   * specified and when the <code>test</code> parameter is not specified, the default includes will be <code><br/>
   * &lt;includes><br/>
   * &nbsp;&lt;include>**&#47;Test*.java&lt;/include><br/>
   * &nbsp;&lt;include>**&#47;*Test.java&lt;/include><br/>
   * &nbsp;&lt;include>**&#47;*Tests.java&lt;/include><br/>
   * &nbsp;&lt;include>**&#47;*TestCase.java&lt;/include><br/>
   * &lt;/includes><br/>
   * </code>
   * <p/>
   * Each include item may also contain a comma-separated sublist of items, which will be treated as multiple
   * &nbsp;&lt;include> entries.<br/>
   * Since 2.19 a complex syntax is supported in one parameter (JUnit 4, JUnit 4.7+, TestNG):<br/>
   * &nbsp;&lt;include>%regex[.*[Cat|Dog].*], Basic????, !Unstable*&lt;/include><br/>
   * &nbsp;&lt;include>%regex[.*[Cat|Dog].*], !%regex[pkg.*Slow.*.class], pkg&#47;**&#47;*Fast*.java&lt;/include><br/>
   * <p/>
   * This parameter is ignored if the TestNG <code>suiteXmlFiles</code> parameter is specified.<br/>
   * <br/>
   * <em>Notice that</em> these values are relative to the directory containing generated test classes of the project
   * being tested. This directory is declared by the parameter <code>testClassesDirectory</code> which defaults
   * to the POM property <code>${project.build.testOutputDirectory}</code>, typically <em>src/test/java</em>
   * unless overridden.
   */
  @Parameter
  private List<String> includes;

  /**
   * (JUnit 4+ providers)
   * The number of times each failing test will be rerun. If set larger than 0, rerun failing tests immediately after
   * they fail. If a failing test passes in any of those reruns, it will be marked as pass and reported as a "flake".
   * However, all the failing attempts will be recorded.
   */
  @Parameter(property = "surefire.rerunFailingTestsCount", defaultValue = "0")
  private int rerunFailingTestsCount;

  /**
   * (TestNG) List of &lt;suiteXmlFile> elements specifying TestNG suite xml file locations. Note that
   * <code>suiteXmlFiles</code> is incompatible with several other parameters of this plugin, like
   * <code>includes/excludes</code>.<br/>
   * This parameter is ignored if the <code>test</code> parameter is specified (allowing you to run a single test
   * instead of an entire suite).
   */
  @Parameter(property = "surefire.suiteXmlFiles")
  private File[] suiteXmlFiles;

  /**
   * Defines the order the tests will be run in. Supported values are "alphabetical", "reversealphabetical", "random",
   * "hourly" (alphabetical on even hours, reverse alphabetical on odd hours), "failedfirst", "balanced" and
   * "filesystem".
   * <br/>
   * <br/>
   * Odd/Even for hourly is determined at the time the of scanning the classpath, meaning it could change during a
   * multi-module build.
   * <br/>
   * <br/>
   * Failed first will run tests that failed on previous run first, as well as new tests for this run.
   * <br/>
   * <br/>
   * Balanced is only relevant with parallel=classes, and will try to optimize the run-order of the tests reducing the
   * overall execution time. Initially a statistics file is created and every next test run will reorder classes.
   * <br/>
   * <br/>
   * Note that the statistics are stored in a file named .surefire-XXXXXXXXX beside pom.xml, and should not be checked
   * into version control. The "XXXXX" is the SHA1 checksum of the entire surefire configuration, so different
   * configurations will have different statistics files, meaning if you change any config settings you will re-run
   * once before new statistics data can be established.
   */
  @Parameter(property = "surefire.runOrder", defaultValue = "filesystem")
  private String runOrder;

  /**
   * A file containing include patterns. Blank lines, or lines starting with # are ignored. If {@code includes} are
   * also specified, these patterns are appended. Example with path, simple and regex includes:<br/>
   * &#042;&#047;test/*<br/>
   * &#042;&#042;&#047;NotIncludedByDefault.java<br/>
   * %regex[.*Test.*|.*Not.*]<br/>
   */
  @Parameter(property = "surefire.includesFile")
  private File includesFile;

  /**
   * A file containing exclude patterns. Blank lines, or lines starting with # are ignored. If {@code excludes} are
   * also specified, these patterns are appended. Example with path, simple and regex excludes:<br/>
   * &#042;&#047;test/*<br/>
   * &#042;&#042;&#047;DontRunTest.*<br/>
   * %regex[.*Test.*|.*Not.*]<br/>
   */
  @Parameter(property = "surefire.excludesFile")
  private File excludesFile;

  /**
   * Set to error/failure count in order to skip remaining tests.
   * Due to race conditions in parallel/forked execution this may not be fully guaranteed.<br/>
   * Enable with system property -Dsurefire.skipAfterFailureCount=1 or any number greater than zero.
   * Defaults to "0".<br/>
   * See the prerequisites and limitations in documentation:<br/>
   * <a href="http://maven.apache.org/plugins/maven-surefire-plugin/examples/skip-after-failure.html">
   *     http://maven.apache.org/plugins/maven-surefire-plugin/examples/skip-after-failure.html</a>
   */
  @Parameter(property = "surefire.skipAfterFailureCount", defaultValue = "0")
  private int skipAfterFailureCount;

  /**
   * After the plugin process is shutdown by sending SIGTERM signal (CTRL+C), SHUTDOWN command is received by every
   * forked JVM. By default (shutdown=testset) forked JVM would not continue with new test which means that
   * the current test may still continue to run.<br/>
   * The parameter can be configured with other two values "exit" and "kill".<br/>
   * Using "exit" forked JVM executes System.exit(1) after the plugin process has received SIGTERM signal.<br/>
   * Using "kill" the JVM executes Runtime.halt(1) and kills itself.
   */
  @Parameter(property = "surefire.shutdown", defaultValue = "testset")
  private String shutdown;

  @Override
  public int getRerunFailingTestsCount() {
    return rerunFailingTestsCount;
  }

  @Override
  protected void handleSummary(RunResult summary, Exception firstForkException)
      throws MojoExecutionException, MojoFailureException {
    SurefireHelper.reportExecution(this, summary, getConsoleLogger(), firstForkException);
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!isSkipExecution()) { // let super.execute() handle the isSkipExecution case
      if (!isForking()) {
        getConsoleLogger().warning("ForkCount=0 is known not to work for GWT tests");
      }
    }
    super.execute();
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
  public List<String> getIncludes() {
    return includes;
  }

  @Override
  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  @Override
  protected String getReportSchemaLocation() {
    return "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd";
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
  public File getReportsDirectory() {
    return reportsDirectory;
  }

  @Override
  public void setReportsDirectory(File reportsDirectory) {
    this.reportsDirectory = reportsDirectory;
  }

  @Override
  public String getTest() {
    return test;
  }

  @Override
  public void setTest(String test) {
    this.test = test;
  }

  @Override
  public int getSkipAfterFailureCount() {
    return skipAfterFailureCount;
  }

  @Override
  public String getShutdown() {
    return shutdown;
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
  public int getForkedProcessExitTimeoutInSeconds() {
    return forkedProcessExitTimeoutInSeconds;
  }

  @Override
  public void setForkedProcessExitTimeoutInSeconds(int forkedProcessExitTimeoutInSeconds) {
    this.forkedProcessExitTimeoutInSeconds = forkedProcessExitTimeoutInSeconds;
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
  public void setFailIfNoSpecifiedTests(boolean failIfNoSpecifiedTests) {
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

  @Override
  public double getParallelTestsTimeoutInSeconds() {
    return parallelTestsTimeoutInSeconds;
  }

  @Override
  public void setParallelTestsTimeoutInSeconds(double parallelTestsTimeoutInSeconds) {
    this.parallelTestsTimeoutInSeconds = parallelTestsTimeoutInSeconds;
  }

  @Override
  public double getParallelTestsTimeoutForcedInSeconds() {
    return parallelTestsTimeoutForcedInSeconds;
  }

  @Override
  public void setParallelTestsTimeoutForcedInSeconds(double parallelTestsTimeoutForcedInSeconds) {
    this.parallelTestsTimeoutForcedInSeconds = parallelTestsTimeoutForcedInSeconds;
  }

  @Override
  public File[] getSuiteXmlFiles() {
    return suiteXmlFiles.clone();
  }

  @Override
  public void setSuiteXmlFiles(File[] suiteXmlFiles) {
    this.suiteXmlFiles = suiteXmlFiles.clone();
  }

  @Override
  public String getRunOrder() {
    return runOrder;
  }

  @Override
  public void setRunOrder(String runOrder) {
    this.runOrder = runOrder;
  }

  @Override
  public File getIncludesFile() {
    return includesFile;
  }

  @Override
  public File getExcludesFile() {
    return excludesFile;
  }

  @Override
  protected List<File> suiteXmlFiles() {
    return hasSuiteXmlFiles() ? Arrays.asList( suiteXmlFiles ) : Collections.<File>emptyList();
  }

  @Override
  protected boolean hasSuiteXmlFiles() {
    return suiteXmlFiles != null && suiteXmlFiles.length != 0;
  }

  // GwtOptions

  @Nullable
  @Override
  public String getLogLevel() {
    return logLevel;
  }

  @Nullable
  @Override
  public String getStyle() {
    return style;
  }

  @Nullable
  @Override
  public Integer getOptimize() {
    return optimize;
  }

  @Override
  public File getWarDir() {
    return outDir;
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

  @Nullable
  @Override
  public String getLocalWorkers() {
    return localWorkers;
  }

  @Nullable
  @Override
  public String getSourceLevel() {
    return sourceLevel;
  }
}
