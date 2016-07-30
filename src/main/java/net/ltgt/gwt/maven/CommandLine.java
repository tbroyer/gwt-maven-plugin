package net.ltgt.gwt.maven;

import java.io.File;
import java.nio.file.Paths;import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

class CommandLine {
  static void execute(final Log log, MavenProject project, Iterable<String> classpath, List<String> arguments) throws MojoExecutionException {
    final String cp = StringUtils.join(classpath.iterator(), File.pathSeparator);
    final String[] args = arguments.toArray(new String[arguments.size()]);

    Commandline commandline = new Commandline();
    commandline.setWorkingDirectory(project.getBuild().getDirectory());
    commandline.setExecutable(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
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
}
