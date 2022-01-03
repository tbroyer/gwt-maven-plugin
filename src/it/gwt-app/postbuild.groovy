import java.util.jar.JarFile

try {
  File target = new File(basedir, "target")
  if (!target.isDirectory()) {
    System.err.println("target file is missing or not a directory.")
    return false
  }

  File moduleFile = new File(target, "classes/it/test/Test.gwt.xml")
  if (!moduleFile.isFile()) {
    System.err.println("it/test/Test.gwt.xml module is missing or is not a file.")
    return false
  }
  String module = moduleFile.text
  if (module.contains("shared")) {
    System.err.println("GWT module has generated sources whereas it contained sources.")
    return false
  }

  File war = new File(target, "gwt-application-1.0.war")
  if (!war.isFile()) {
    System.err.println("war file is missing or a not a file.")
    return false
  }

  JarFile jarFile = new JarFile(war)
  if (jarFile.getEntry("test/test.nocache.js") == null) {
    System.err.println("test/test.nocache.js missing from war")
    return false
  }
  if (jarFile.stream().anyMatch { it.name.startsWith("WEB-INF/") }) {
    System.err.println("war file erroneously contains a WEB-INF/")
    return false
  }

  File buildLogFile = new File(basedir, "build.log")
  if (!buildLogFile.exists() || buildLogFile.isDirectory()) {
    System.err.println("build.log file is missing or a directory.")
    return false
  }
  String buildLog = buildLogFile.text
  if (buildLog.contains("build is platform dependent")) {
    System.err.println("Encoding is not set.")
    return false
  }
  if (!buildLog.contains("Tests run: 3, Failures: 0, Errors: 0, Skipped: 0")) {
    System.err.println("build.log does not talk about running tests")
    return false
  }
} catch (Throwable t) {
  t.printStackTrace()
  return false
}

return true
