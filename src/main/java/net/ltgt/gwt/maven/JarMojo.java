package net.ltgt.gwt.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Package the compiled GWT library into a JAR archive.
 */
@Mojo(name = "package-lib", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class JarMojo extends AbstractMojo {
  /**
   * Directory containing the classes and resource files that should be packaged into the JAR.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  /**
   * Directory containing the generated JAR.
   */
  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File outputDirectory;

  /**
   * Name of the generated JAR.
   */
  @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
  private String finalName;

  /**
   * The Jar archiver.
   */
  @Component(role = Archiver.class, hint = "jar" )
  private JarArchiver jarArchiver;

  /**
   * The {@link {MavenProject}.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * The {@link MavenSession}.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  /**
   * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
   * Archiver Reference</a>.
   */
  @Parameter
  private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

  @Component
  private MavenProjectHelper projectHelper;

  /**
   * Require the GWT plugin to build a new JAR even if none of the contents appear to have changed. By default, this
   * plugin looks to see if the output jar exists and inputs have not changed. If these conditions are true, the
   * plugin skips creation of the jar. This does not work when other plugins, like the maven-shade-plugin, are
   * configured to post-process the jar. This plugin can not detect the post-processing, and so leaves the
   * post-processed jar in place. This can lead to failures when those plugins do not expect to find their own output
   * as an input. Set this parameter to <tt>true</tt> to avoid these problems by forcing this plugin to recreate the
   * jar every time.<br/>
   */
  @Parameter(property = "maven.jar.forceCreation", defaultValue = "false")
  private boolean forceCreation;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File jarFile = new File(outputDirectory, finalName + ".jar");

    MavenArchiver archiver = new MavenArchiver();
    archiver.setArchiver(jarArchiver);
    archiver.setOutputFile(jarFile);

    archive.setForced(forceCreation);

    List<String> sourceRoots = SourcesAsResourcesHelper.filterSourceRoots(
        getLog(), project.getResources(), project.getCompileSourceRoots());

    try {
      for (String sourceRoot : sourceRoots) {
        File f = new File(sourceRoot);
        if (f.exists()) {
          jarArchiver.addDirectory(f);
        }
      }

      if (classesDirectory.exists()) {
        jarArchiver.addDirectory(classesDirectory);
      }

      archiver.createArchive(session, project, archive);
    } catch (Exception e) {
      throw new MojoExecutionException("Error packaging GWT library", e);
    }

    project.getArtifact().setFile(jarFile);
  }
}
