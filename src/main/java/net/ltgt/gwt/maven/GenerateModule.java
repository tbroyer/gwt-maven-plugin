package net.ltgt.gwt.maven;

import com.google.common.base.Charsets;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;
import com.google.gwt.dev.cfg.ModuleDef;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mojo(name = "generate-module", threadSafe = true, defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateModule extends AbstractMojo {

  /**
   * The directory where the GWT module descriptor will be generated.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}")
  private File outputDirectory;

  /**
   * The module to generate.
   */
  @Parameter
  private String moduleName;

  /**
   * The short name of the module, used to name the output {@code .nocache.js} file.
   */
  @Parameter
  private String moduleShortName;

  /**
   * Modules to inherit in addition to the ones detected from dependencies.
   */
  @Parameter
  private List<String> inheritedModules;

  /**
   * Name of the EntryPoint class for the module.
   */
  @Parameter
  private String entryPointClass;

  /**
   * A flag to disable generation of the GWT module in favor of a hand-authored module descriptor.
   */
  @Parameter(defaultValue = "false")
  private boolean skipModule;

  @Parameter(defaultValue = "${project.dependencyArtifacts}", required = true, readonly = true)
  private Set<Artifact> dependencyArtifacts;

  private final ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE_PLUS_RUNTIME);

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipModule) {
      return;
    }

    if (StringUtils.isBlank(moduleName)) {
      throw new MojoExecutionException("Missing moduleName");
    }

    if (!ModuleDef.isValidModuleName(moduleName)) {
      throw new MojoExecutionException("Invalid module name: " + moduleName);
    }

    ClassWorld world = new ClassWorld();
    ClassRealm  realm;
    try {
      realm = world.newRealm("gwt", null);
      for (Artifact artifact : dependencyArtifacts) {
        if (!artifactFilter.include(artifact)) {
          continue;
        }
        if (!artifact.getArtifactHandler().isAddedToClasspath()) {
          continue;
        }
        realm.addURL(artifact.getFile().toURI().toURL());
      }
    } catch (DuplicateRealmException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (MalformedURLException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    LinkedHashSet<String> moduleNames = new LinkedHashSet<String>();
    try {
      Enumeration<URL> resources = realm.getResources("META-INF/gwt/mainModule");
      while (resources.hasMoreElements()) {
        final URL resource = resources.nextElement();
        String moduleName = Resources.readLines(resource, Charsets.UTF_8, new LineProcessor<String>() {
          private String module;

          @Override
          public boolean processLine(String line) throws IOException {
            line = StringUtils.substringBefore(line, "#").trim();
            if (line.isEmpty()) {
              return true;
            }
            if (module != null) {
              getLog().warn("Configuration file contains more than one module name, picking first: " + resource);
              return false;
            }
            if (!ModuleDef.isValidModuleName(line)) {
              getLog().warn("Illegal configuration-file syntax, skipping " + resource);
              return false;
            }
            module = line;
            // Continue processing lines to warn of illegal syntax
            return true;
          }

          @Override
          public String getResult() {
            return module;
          }
        });
        moduleNames.add(moduleName);
      }
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    File outputFile = new File(outputDirectory, moduleName.replace('.', '/') + ".gwt.xml");
    outputFile.getParentFile().mkdirs();
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8);
      XMLWriter xmlWriter = new PrettyPrintXMLWriter(writer);

      xmlWriter.startElement("module");
      if (!StringUtils.isBlank(moduleShortName)) {
        xmlWriter.addAttribute("rename-to", moduleShortName);
      }

      for (String module : moduleNames) {
        xmlWriter.startElement("inherits");
        xmlWriter.addAttribute("name", module);
        xmlWriter.endElement();
      }

      if (inheritedModules != null) {
        for (String inheritedModule : inheritedModules) {
          xmlWriter.startElement("inherits");
          xmlWriter.addAttribute("name", inheritedModule);
          xmlWriter.endElement();
        }
      }

      // <entry-point class="${entryPointClass}" />
      if (!StringUtils.isBlank(entryPointClass)) {
        xmlWriter.startElement("entry-point");
        xmlWriter.addAttribute("class", entryPointClass);
        xmlWriter.endElement();
      }

      // <source path="client" />
      xmlWriter.startElement("source");
      xmlWriter.addAttribute("path", "client");
      xmlWriter.endElement();
      // <super-source path="super" />
      xmlWriter.startElement("super-source");
      xmlWriter.addAttribute("path", "super");
      xmlWriter.endElement();

      // TODO: custom configuration (deferred binding, linkers, etc.)

      xmlWriter.endElement(); // module
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } finally {
      IOUtil.close(writer);
    }
  }
}
