package net.ltgt.gwt.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Generates a GWT module definition from Maven dependencies, or merge {@code <inherits/>} with a module template.
 * <p>
 * When no module template exist, the behavior is identical to using an empty file.
 * <p>
 * {@code META-INF/gwt/mainModule} files from the project dependencies (<b>not</b> transitive) are used to generate
 * {@code <inherits/>} directives. Those directives are inserted at the very beginning of the generated module
 * (notably, they'll appear before any existing {@code <inherits/>} directive in the module template).
 * <p>
 * If {@code moduleShortName} is specified (and not empty), it <b>overwrites</b> any existing {@code rename-to} from
 * the module template.
 * <p>
 * Unless the module template contains a source folder (either {@code <source/>} or {@code <super-source/>}, those
 * three lines will be inserted at the very end of the generated module (this is to keep any {@code includes} or
 * {@code excludes} or specific {@code path} from the module template):
 * <pre><code>
 * &lt;source path="client"/>
 * &lt;source path="shared"/>
 * &lt;super-source path="super"/>
 * </code></pre>
 */
@Mojo(name = "generate-module", threadSafe = true, defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateModuleMojo extends AbstractMojo {

  /**
   * The directory where the GWT module descriptor will be generated.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
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
   * Module definition to merge with.
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/module.gwt.xml")
  private File moduleTemplate;

  /**
   * A flag to disable generation of the GWT module in favor of a hand-authored module descriptor.
   */
  @Parameter(defaultValue = "false")
  private boolean skipModule;

  /**
   * A flag to enable generation of {@code <inherits/>} from {@code META-INF/gwt/mainModule} files
   * in dependencies.
   */
  @Parameter(defaultValue = "true")
  private boolean generateInheritsFromDependencies = true;

  @Parameter(defaultValue = "${project.dependencyArtifacts}", required = true, readonly = true)
  private Set<Artifact> dependencyArtifacts;

  @Component
  private BuildContext buildContext;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  private final ScopeArtifactFilter artifactFilter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE_PLUS_RUNTIME);

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipModule) {
      return;
    }

    if (StringUtils.isBlank(moduleName)) {
      throw new MojoExecutionException("Missing moduleName");
    }

//    if (!ModuleDef.isValidModuleName(moduleName)) {
//      throw new MojoExecutionException("Invalid module name: " + moduleName);
//    }

    File outputFile = new File(outputDirectory, moduleName.replace('.', '/') + ".gwt.xml");

    boolean uptodate;
    Xpp3Dom template;
    if (moduleTemplate != null && moduleTemplate.isFile()) {
      uptodate = buildContext.isUptodate(outputFile, moduleTemplate);
      try {
        template = Xpp3DomBuilder.build(
          new BufferedReader(new InputStreamReader(new FileInputStream(moduleTemplate), StandardCharsets.UTF_8)));
      } catch (XmlPullParserException | IOException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }
    } else {
      uptodate = true;
      template = new Xpp3Dom("module");
    }

    if (uptodate) {
      uptodate = buildContext.isUptodate(outputFile, project.getFile());
    }

    if (uptodate) {
      // TODO: check dependencies (META-INF/gwt/mainModule might have changed)
      // For now, we'll compare the content of the file before writing it
//      getLog().info("Module is up to date; skipping.");
//      return;
    }

    outputFile.getParentFile().mkdirs();
    Writer writer = null;
    try {
      writer = new StringWriter();
      XMLWriter xmlWriter = new PrettyPrintXMLWriter(writer);

      xmlWriter.startElement("module");
      // override or copy rename-to
      String oldRenameTo = template.getAttribute("rename-to");
      if (!StringUtils.isBlank(moduleShortName)) {
        if (oldRenameTo != null) {
          getLog().info("Overriding module short name " + oldRenameTo + " with " + moduleShortName);
        }
        xmlWriter.addAttribute("rename-to", moduleShortName);
      } else if (oldRenameTo != null) {
        xmlWriter.addAttribute("rename-to", oldRenameTo);
      }
      // copy other attributes
      for (String attrName : template.getAttributeNames()) {
        if ("rename-to".equals(attrName)) {
          continue;
        }
        xmlWriter.addAttribute(attrName, template.getAttribute(attrName));
      }

      boolean hasInherits = generateInheritsFromDependencies(xmlWriter);

      // copy children
      boolean hasSource = false;
      for (Xpp3Dom child : template.getChildren()) {
        if ("inherits".equals(child.getName())) {
          hasInherits = true;
        } else if ("source".equals(child.getName()) || "super-source".equals(child.getName())) {
          hasSource = true;
        }
        Xpp3DomWriter.write(xmlWriter, child);
      }

      // insert <inherits name="com.google.gwt.core.Core"/> if no other inherited module
      if (!hasInherits) {
        xmlWriter.startElement("inherits");
        xmlWriter.addAttribute("name", "com.google.gwt.core.Core");
        xmlWriter.endElement();
      }

      if (!hasSource) {
        // <source path="client" />
        xmlWriter.startElement("source");
        xmlWriter.addAttribute("path", "client");
        xmlWriter.endElement();
        // <source path="shared" />
        xmlWriter.startElement("source");
        xmlWriter.addAttribute("path", "shared");
        xmlWriter.endElement();
        // <super-source path="super" />
        xmlWriter.startElement("super-source");
        xmlWriter.addAttribute("path", "super");
        xmlWriter.endElement();
      }

      xmlWriter.endElement(); // module
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } finally {
      IOUtil.close(writer);
    }

    try {
      if (outputFile.isFile() && writer.toString().equals(FileUtils.fileRead(outputFile, "UTF-8"))) {
        getLog().info(outputFile.getAbsolutePath() + " up to date - skipping");
        return;
      }

      FileUtils.fileWrite(outputFile, "UTF-8", writer.toString());
      buildContext.refresh(outputFile);
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private boolean generateInheritsFromDependencies(XMLWriter xmlWriter) throws IOException, MojoExecutionException {
    if (!generateInheritsFromDependencies) {
      return false;
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
    } catch (DuplicateRealmException | MalformedURLException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    boolean hasInherits = false;

    Enumeration<URL> resources = realm.getResources("META-INF/gwt/mainModule");
    while (resources.hasMoreElements()) {
      final URL resource = resources.nextElement();
      String moduleName = null;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
          line = StringUtils.substringBefore(line, "#").trim();
          if (line.isEmpty()) {
            continue;
          }
          if (moduleName != null) {
            getLog().warn("Configuration file contains more than one module name, picking first: " + resource);
            break;
          }
//        if (!ModuleDef.isValidModuleName(moduleName)) {
//          getLog().warn("Illegal configuration-file syntax, skipping " + resource);
//          break;
//        }
          moduleName = line;
          // Continue processing lines to warn of illegal syntax
        }
      }

      if (moduleName != null) {
        hasInherits = true;

        xmlWriter.startElement("inherits");
        xmlWriter.addAttribute("name", moduleName);
        xmlWriter.endElement();
      }
    }

    return hasInherits;
  }
}
