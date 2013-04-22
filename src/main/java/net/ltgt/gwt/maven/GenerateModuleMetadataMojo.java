package net.ltgt.gwt.maven;

import com.google.gwt.dev.cfg.ModuleDef;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.IOException;

@Mojo(name = "generate-module-metadata", threadSafe = true, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateModuleMetadataMojo extends AbstractMojo {

  /**
   * The main GWT module of the project.
   */
  @Parameter
  private String moduleName;

  /**
   * The directory where the generated {@code mainModule} file will be put.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/gwt")
  private File outputDirectory;

  /**
   * A flag to disable generation of the {@code mainModule}.
   */
  @Parameter(defaultValue = "false")
  private boolean skipModuleMetadata;

  @Component
  private BuildContext buildContext;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipModuleMetadata) {
      return;
    }

    if (StringUtils.isBlank(moduleName)) {
      throw new MojoExecutionException("Missing moduleName");
    }

    if (!ModuleDef.isValidModuleName(moduleName)) {
      throw new MojoExecutionException("Invalid module name: " + moduleName);
    }

    File outputFile = new File(outputDirectory, "mainModule");
    if (outputFile.isFile()) {
      try {
        String content = FileUtils.fileRead(outputFile, "UTF-8");
        if (content.trim().equals(moduleName)) {
          getLog().info(outputFile.getAbsolutePath() + " up to date - skipping.");
          return;
        }
      } catch (IOException e) {
        // fall-through; let's try writing.
      }
    }

    outputDirectory.mkdirs();
    try {
      FileUtils.fileWrite(outputFile, "UTF-8", moduleName);
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    buildContext.refresh(outputFile);
  }
}
