package net.ltgt.gwt.maven;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
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
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

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
   * Module definition to merge with.
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/module.gwt.xml")
  private File moduleTemplate;

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

    Xpp3Dom template;
    if (moduleTemplate.isFile()) {
      try {
        template = Xpp3DomBuilder.build(Files.newReader(moduleTemplate, Charsets.UTF_8));
      } catch (XmlPullParserException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      } catch (IOException e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }
    } else {
      template = new Xpp3Dom("module");
    }

    File outputFile = new File(outputDirectory, moduleName.replace('.', '/') + ".gwt.xml");
    outputFile.getParentFile().mkdirs();
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8);
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
  }

  private boolean generateInheritsFromDependencies(XMLWriter xmlWriter) throws MojoExecutionException {
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

    boolean hasInherits = false;
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

        xmlWriter.startElement("inherits");
        xmlWriter.addAttribute("name", moduleName);
        xmlWriter.endElement();
      }
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    return hasInherits;
  }
}
