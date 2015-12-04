package net.ltgt.gwt.maven;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
import org.apache.maven.surefire.util.DefaultScanResult;
import org.apache.maven.surefire.util.NestedCheckedException;
import org.codehaus.plexus.util.StringUtils;

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
  @Parameter(property = "gwt.style", defaultValue = "OBFUSCATED")
  private Style style;

  /**
   * Runs tests in Development Mode, using the Java virtual machine.
   */
  @Parameter(property = "gwt.test.devMode", defaultValue = "true")
  private boolean devMode;

  /**
   * Compile quickly with minimal optimizations.
   */
  @Parameter(property = "gwt.draftCompile", defaultValue = "false")
  private boolean draftCompile;

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
      props = new HashMap<String, String>(2);
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
      sb.append(devMode ? " -devMode" : " -nodevMode");
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
    checksum.add(style.name());
    checksum.add(draftCompile);
    checksum.add(localWorkers);
    checksum.add(devMode);
    checksum.add(sourceLevel);
    checksum.add(testArgs);
    checksum.add(useCompilerArgsForTests);
    if (useCompilerArgsForTests) {
      checksum.add(compilerArgs);
    }
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
   * You can execute a limited number of methods in the test by
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
   */
  @Parameter(property = "surefire.failIfNoSpecifiedTests")
  private Boolean failIfNoSpecifiedTests;

  /**
   * Attach a debugger to the forked JVM. If set to "true", the process will
   * suspend and wait for a debugger to attach on port 5005. If set to some
   * other string, that string will be appended to the argLine, allowing you to
   * configure arbitrary debuggability options (without overwriting the other
   * options specified through the <code>argLine</code> parameter).
   */
  @Parameter(property = "maven.surefire.debug")
  private String debugForkedProcess;

  /**
   * Kill the forked test process after a certain number of seconds. If set to
   * 0, wait forever for the process, never timing out.
   */
  @Parameter(property = "surefire.timeout")
  private int forkedProcessTimeoutInSeconds;

  /**
   * A list of &lt;include> elements specifying the tests (by pattern) that should be included in testing. When not
   * specified and when the <code>test</code> parameter is not specified, the default includes will be <code><br/>
   * &lt;includes><br/>
   * &nbsp;&lt;include>**&#47;*Suite.java&lt;/include><br/>
   * &nbsp;&lt;include>**&#47;*SuiteNoBrowser.java&lt;/include><br/>
   * &lt;/includes><br/>
   * </code>
   * <p/>
   * Each include item may also contain a comma-separated sublist of items, which will be treated as multiple
   * &nbsp;&lt;include> entries.<br/>
   * <p/>
   * This parameter is ignored if the TestNG <code>suiteXmlFiles</code> parameter is specified.
   */
  @Parameter
  private List<String> includes;

  /**
   * Stop executing queued parallel JUnit tests after a certain number of seconds.
   * If set to 0, wait forever, never timing out.
   * Makes sense with specified <code>parallel</code> different from "none".
   */
  @Parameter( property = "failsafe.parallel.timeout" )
  private int parallelTestsTimeoutInSeconds;

  /**
   * Stop executing queued parallel JUnit tests
   * and <em>interrupt</em> currently running tests after a certain number of seconds.
   * If set to 0, wait forever, never timing out.
   * Makes sense with specified <code>parallel</code> different from "none".
   */
  @Parameter( property = "failsafe.parallel.forcedTimeout" )
  private int parallelTestsTimeoutForcedInSeconds;

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
  protected void executeAfterPreconditionsChecked(DefaultScanResult scanResult) throws MojoExecutionException, MojoFailureException {
    if (getEffectiveForkCount() <= 0) {
      getLog().warn("ForkCount=0 is known not to work for GWT tests");
    }
    super.executeAfterPreconditionsChecked(scanResult);
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

  @Override
  public int getParallelTestsTimeoutInSeconds() {
    return parallelTestsTimeoutInSeconds;
  }

  @Override
  public void setParallelTestsTimeoutInSeconds(int parallelTestsTimeoutInSeconds) {
    this.parallelTestsTimeoutInSeconds = parallelTestsTimeoutInSeconds;
  }

  @Override
  public int getParallelTestsTimeoutForcedInSeconds() {
    return parallelTestsTimeoutForcedInSeconds;
  }

  @Override
  public void setParallelTestsTimeoutForcedInSeconds(int parallelTestsTimeoutForcedInSeconds) {
    this.parallelTestsTimeoutForcedInSeconds = parallelTestsTimeoutForcedInSeconds;
  }

  // GwtOptions

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

  @Override
  public int getLocalWorkers() {
    return localWorkers;
  }

  @Override
  public String getSourceLevel() {
    return sourceLevel;
  }
}
