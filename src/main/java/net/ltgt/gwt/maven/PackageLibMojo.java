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
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;

@Mojo(name = "package-lib", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PackageLibMojo extends AbstractMojo {

  /**
   * Directory containing the generated JAR.
   */
  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File outputDirectory;

  /**
   * Name of the generated JAR.
   */
  @Parameter(alias = "jarName", property = "jar.finalName", defaultValue = "${project.build.finalName}")
  private String finalName;

  /**
   * Directory containing the classes and resource files that should be packaged
   * into the JAR.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  /**
   * Require the jar plugin to build a new JAR even if none of the contents
   * appear to have changed. By default, this plugin looks to see if the output
   * jar exists and inputs have not changed. If these conditions are true, the
   * plugin skips creation of the jar. This does not work when other plugins,
   * like the maven-shade-plugin, are configured to post-process the jar. This
   * plugin can not detect the post-processing, and so leaves the post-processed
   * jar in place. This can lead to failures when those plugins do not expect to
   * find their own output as an input. Set this parameter to <tt>true</tt> to
   * avoid these problems by forcing this plugin to recreate the jar every time.
   */
  @Parameter(property = "jar.forceCreation", defaultValue = "false")
  private boolean forceCreation;

  /**
   * The archive configuration to use. See <a
   * href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
   * Archiver Reference</a>.
   */
  @Parameter
  private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

  /**
   * Path to the default MANIFEST file to use. It will be used if
   * <code>useDefaultManifestFile</code> is set to <code>true</code>.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", required = true, readonly = true)
  private File defaultManifestFile;

  /**
   * Set this to <code>true</code> to enable the use of the
   * <code>defaultManifestFile</code>.
   */
  @Parameter(property = "jar.useDefaultManifestFile", defaultValue = "false")
  private boolean useDefaultManifestFile;

  @Component
  private MavenProject project;

  @Component
  private MavenSession session;

  @Component(role = Archiver.class, hint = "jar")
  private JarArchiver jarArchiver;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File warFile = new File(outputDirectory, finalName + ".war");

    MavenArchiver archiver = new MavenArchiver();
    archiver.setArchiver(jarArchiver);
    archiver.setOutputFile(warFile);

    archive.setForced(forceCreation);

    try {
      jarArchiver.addDirectory(classesDirectory);

      for (String compileSourceRoot : project.getCompileSourceRoots()) {
        jarArchiver.addDirectory(new File(compileSourceRoot));
      }

      if (useDefaultManifestFile && defaultManifestFile.exists() && archive.getManifestFile() == null) {
        getLog().info("Adding existing MANIFEST to archive. Found under: " + defaultManifestFile.getPath());
        archive.setManifestFile(defaultManifestFile);
      }

      archiver.createArchive(session, project, archive);
    } catch (Exception e) {
      throw new MojoExecutionException("Error packaging GWT application", e);
    }

    project.getArtifact().setFile(warFile);
  }
}
