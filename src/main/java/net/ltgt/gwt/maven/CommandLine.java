package net.ltgt.gwt.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class CommandLine {
  static void execute(final Log log, MavenProject project, Iterable<String> classpath, List<String> arguments) throws MojoExecutionException {
    final String cp = StringUtils.join(classpath.iterator(), File.pathSeparator);

    final List<String> cmd = new ArrayList<>();
    cmd.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
    cmd.addAll(arguments);

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(Paths.get(project.getBuild().getDirectory()).toFile());
    pb.environment().put("CLASSPATH", cp);
    pb.inheritIO();
    
    if (log.isDebugEnabled()) {
      log.debug("Classpath: " + cp);
      log.debug("Command: " + StringUtils.join(cmd.iterator(), " "));
    }
    
    int result;

    try {
      Process p = pb.start();
      result = p.waitFor();
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    if (result != 0) {
      throw new MojoExecutionException("GWT exited with status " + result);
    }
  }
}
