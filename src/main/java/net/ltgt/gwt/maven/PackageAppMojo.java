package net.ltgt.gwt.maven;

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
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.war.WarArchiver;

import java.io.File;

@Mojo( name = "package-app", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class PackageAppMojo extends AbstractMojo {

  /**
   * The directory for the generated WAR.
   */
  @Parameter( defaultValue = "${project.build.directory}", required = true )
  private String outputDirectory;

  /**
   * The name of the generated WAR.
   */
  @Parameter( defaultValue = "${project.build.finalName}", required = true )
  private String warName;

  /**
   * Require the GWT plugin to build a new WAR even if none of the contents
   * appear to have changed. By default, this plugin looks to see if the output
   * war exists and inputs have not changed. If these conditions are true, the
   * plugin skips creation of the war. This does not work when other plugins,
   * like the maven-shade-plugin, are configured to post-process the war. This
   * plugin can not detect the post-processing, and so leaves the post-processed
   * war in place. This can lead to failures when those plugins do not expect to
   * find their own output as an input. Set this parameter to <tt>true</tt> to
   * avoid these problems by forcing this plugin to recreate the war every time.
   */
  @Parameter(property = "war.forceCreation", defaultValue = "false")
  private boolean forceCreation;

  /**
   * The archive configuration to use.
   * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
   */
  @Parameter
  private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

  @Component
  private MavenProject project;

  @Component
  private MavenSession session;

  @Component( role = Archiver.class, hint = "war" )
  private WarArchiver warArchiver;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // ignoreWebxml works backwards! See http://jira.codehaus.org/browse/PLXCOMP-45
    warArchiver.setIgnoreWebxml(false);

    File warFile = new File(outputDirectory, warName + ".war");

    MavenArchiver archiver = new MavenArchiver();
    archiver.setArchiver(warArchiver);
    archiver.setOutputFile(warFile);

    archive.setForced(forceCreation);

    try {
      File prepackagedApp = new File(outputDirectory, warName);
      if (prepackagedApp.exists()) {
        warArchiver.addDirectory(prepackagedApp);
      }

      archiver.createArchive(session, project, archive);
    } catch (Exception e) {
      throw new MojoExecutionException("Error packaging GWT application", e);
    }

    project.getArtifact().setFile(warFile);
  }
}
