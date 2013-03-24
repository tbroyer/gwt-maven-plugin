import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.codehaus.plexus.util.*;

try {
  File modelModuleFile = new File(basedir, "e2e-model-gwt/target/classes/it/test/model/TestModel.gwt.xml");
  if (!modelModuleFile.isFile()) {
    System.err.println("it/test/model/TestModel.gwt.xml module is missing or is not a file.");
    return false;
  }
  String modelModule = FileUtils.fileRead(modelModuleFile);
  if (modelModule.contains("<super-source")) {
    System.err.println("Model GWT module has generated sources whereas it contained sources.");
    return false;
  }

  File sharedModuleFile = new File(basedir, "e2e-shared-gwt/target/classes/it/test/E2EShared.gwt.xml");
  if (!sharedModuleFile.isFile()) {
    System.err.println("it/test/E2EShared.gwt.xml module is missing or is not a file.");
    return false;
  }
  String sharedModule = FileUtils.fileRead(sharedModuleFile);
  if (!sharedModule.contains("it.test.model.TestModel")) {
    System.err.println("Shared GWT module doesn't inherit it.test.model.TestModel.");
    return false;
  }

  File clientModuleFile = new File(basedir, "e2e-client/target/classes/it/test/E2E.gwt.xml");
  if (!clientModuleFile.isFile()) {
    System.err.println("it/test/E2E.gwt.xml module is missing or is not a file.");
    return false;
  }
  String clientModule = FileUtils.fileRead(clientModuleFile);
  if (!clientModule.contains("it.test.E2EShared")) {
    System.err.println("Client GWT module doesn't inherit it.test.E2EShared.");
    return false;
  }

  File target = new File(basedir, "e2e-server/target");
  if (!target.exists() || !target.isDirectory()) {
    System.err.println("target file is missing or not a directory.");
    return false;
  }

  File war = new File(target, "e2e-server-1.0.war");
  if (!war.exists() || war.isDirectory()) {
    System.err.println("war file is missing or a directory.");
    return false;
  }

  JarFile jarFile = new JarFile(war);
  Enumeration entries = jarFile.entries();

  boolean seenNocacheJs = false;
  boolean seenGwtLib = false;
  while (entries.hasMoreElements()) {
    JarEntry entry = entries.nextElement();
    String name = entry.getName();
    if (name.equals("e2e/e2e.nocache.js")) {
      seenNocacheJs = true;
    } else if (name.startsWith("WEB-INF/lib/e2e-") && name.endsWith("-gwt.jar")) {
      seenGwtLib = true;
    }
  }
  if (!seenNocacheJs) {
    System.err.println("e2e/e2e.nocache.js missing from final war");
    return false;
  }
  if (seenGwtLib) {
    System.err.println("gwt-lib erroneously packaged into final war");
    return false;
  }
} catch (Throwable t) {
  t.printStackTrace();
  return false;
}

return true;