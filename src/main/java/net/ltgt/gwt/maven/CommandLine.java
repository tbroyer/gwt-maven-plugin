package net.ltgt.gwt.maven;

import java.io.File;
import java.nio.file.Paths;import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

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

    Commandline commandline = new Commandline();
    commandline.setWorkingDirectory(project.getBuild().getDirectory());
    commandline.setExecutable(getExecutable());
    commandline.addEnvironment("CLASSPATH", cp);
    commandline.addArguments(args);

    if (log.isDebugEnabled()) {
      log.debug("Classpath: " + cp);
      log.debug("Arguments: " + CommandLineUtils.toString(args));
    }

    int result;
    try {
      result = CommandLineUtils.executeCommandLine(commandline,
          new StreamConsumer() {
            @Override
            public void consumeLine(String s) {
              log.info(s);
            }
          },
          new StreamConsumer() {
            @Override
            public void consumeLine(String s) {
              log.warn(s);
            }
          });
    } catch (CommandLineException e) {
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

  @Nullable
  private Toolchain getToolchain() {
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
