package net.ltgt.gwt.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import org.jspecify.annotations.Nullable;

class CommandLine {
  private final Log log;
  private final MavenProject project;
  private final MavenSession session;
  private final ToolchainManager toolchainManager;
  private final Map<String, String> toolchainRequirements;
  private final String jvm;

  CommandLine(Log log, MavenProject project, MavenSession session, ToolchainManager toolchainManager, Map<String, String> toolchainRequirements, String jvm) {
    this.log = log;
    this.project = project;
    this.session = session;
    this.toolchainManager = toolchainManager;
    this.toolchainRequirements = toolchainRequirements;
    this.jvm = jvm;
  }


  void execute(Iterable<String> classpath, List<String> arguments) throws MojoExecutionException {
    final String cp = StringUtils.join(classpath.iterator(), File.pathSeparator);
    final String[] args = arguments.toArray(new String[arguments.size()]);

    org.apache.commons.exec.CommandLine commandline = new org.apache.commons.exec.CommandLine(getExecutable());
    commandline.addArguments(args);

    Executor executor = DefaultExecutor.builder().get();
    executor.setWorkingDirectory(new File(project.getBuild().getDirectory()));
    executor.setStreamHandler(
        new PumpStreamHandler(
            new LogOutputStream() {
              @Override
              protected void processLine(String line, int logLevel) {
                log.info(line);
              }
            },
            new LogOutputStream() {
              @Override
              protected void processLine(String line, int logLevel) {
                log.warn(line);
              }
            }));
    executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());

    Map<String, String> env = new LinkedHashMap<>(System.getenv());
    env.put("CLASSPATH", cp);

    if (log.isDebugEnabled()) {
      log.debug("Classpath: " + cp);
      log.debug("Arguments: " + String.join(" ", commandline.getArguments()));
    }

    int result;
    try {
      result = executor.execute(commandline, env);
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    if (result != 0) {
      throw new MojoExecutionException("GWT exited with status " + result);
    }
  }

  private String getExecutable() {
    if (StringUtils.isNotBlank(jvm)) {
      return jvm;
    }
    Toolchain tc = getToolchain();
    if (tc != null) {
      String executable = tc.findTool("java");
      if (StringUtils.isNotBlank(executable)) {
        if (log.isDebugEnabled()) {
          log.debug("Toolchain: " + tc);
        }
        return executable;
      } // fallthrough
    }
    return Paths.get(System.getProperty("java.home"), "bin", "java").toString();
  }

  private @Nullable Toolchain getToolchain() {
    Toolchain tc = null;
    if (toolchainRequirements != null && !toolchainRequirements.isEmpty()) {
      List<Toolchain> tcs = toolchainManager.getToolchains(session, "jdk", toolchainRequirements);
      if (tcs != null && !tcs.isEmpty()) {
        tc = tcs.get(0);
      }
    }
    if (tc == null) {
      tc = toolchainManager.getToolchainFromBuildContext("jdk", session);
    }
    return tc;
  }
}
