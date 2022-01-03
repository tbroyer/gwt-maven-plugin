import java.util.jar.JarFile

try {
  File modelModuleFile = new File(basedir, "e2e-model-gwt/target/classes/it/test/model/TestModel.gwt.xml")
  if (!modelModuleFile.isFile()) {
    System.err.println("it/test/model/TestModel.gwt.xml module is missing or is not a file.")
    return false
  }
  if (modelModuleFile.text.contains("<super-source")) {
    System.err.println("Model GWT module has generated sources whereas it contained sources.")
    return false
  }

  File sharedModuleFile = new File(basedir, "e2e-shared-gwt/target/classes/it/test/E2EShared.gwt.xml")
  if (!sharedModuleFile.isFile()) {
    System.err.println("it/test/E2EShared.gwt.xml module is missing or is not a file.")
    return false
  }
  if (!sharedModuleFile.text.contains("it.test.model.TestModel")) {
    System.err.println("Shared GWT module doesn't inherit it.test.model.TestModel.")
    return false
  }

  File clientModuleFile = new File(basedir, "e2e-client/target/classes/it/test/E2E.gwt.xml")
  if (!clientModuleFile.isFile()) {
    System.err.println("it/test/E2E.gwt.xml module is missing or is not a file.")
    return false
  }
  if (!clientModuleFile.text.contains("it.test.E2EShared")) {
    System.err.println("Client GWT module doesn't inherit it.test.E2EShared.")
    return false
  }

  File target = new File(basedir, "e2e-server/target")
  if (!target.exists() || !target.isDirectory()) {
    System.err.println("target file is missing or not a directory.")
    return false
  }

  File war = new File(target, "e2e-server-1.0.war")
  if (!war.exists() || war.isDirectory()) {
    System.err.println("war file is missing or a directory.")
    return false
  }

  JarFile jarFile = new JarFile(war)
  if (jarFile.getEntry("e2e/e2e.nocache.js") == null) {
    System.err.println("e2e/e2e.nocache.js missing from final war")
    return false
  }
  if (jarFile.stream().anyMatch { it.name.startsWith("WEB-INF/lib/e2e-") && it.name.endsWith("-gwt.jar") }) {
    System.err.println("gwt-lib erroneously packaged into final war")
    return false
  }
} catch (Throwable t) {
  t.printStackTrace()
  return false
}

return true
