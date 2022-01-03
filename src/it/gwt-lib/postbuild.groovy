import java.util.jar.JarFile

try {
  File target = new File(basedir, "target")
  if (!target.exists() || !target.isDirectory()) {
    System.err.println("target file is missing or not a directory.")
    return false
  }

  File moduleFile = new File(target, "classes/it/testlib/TestLib.gwt.xml")
  if (!moduleFile.isFile()) {
    System.err.println("it/testlib/TestLib.gwt.xml module is missing or is not a file.")
    return false
  }
  if (!moduleFile.text.contains("<super-source")) {
    System.err.println("GWT module does not have generated sources.")
    return false
  }

  File jar = new File(target, "gwt-library-1.0.jar")
  if (!jar.exists() || jar.isDirectory()) {
    System.err.println("jar file is missing or a directory.")
    return false
  }

  JarFile jarFile = new JarFile(jar)
  Enumeration entries = jarFile.entries()

  boolean result = true
  if (jarFile.getEntry("META-INF/gwt/mainModule") == null) {
    System.err.println("META-INF/gwt/mainModule missing from jar")
    result = false
  }
  if (jarFile.getEntry("it/testlib/TestLib.gwt.xml") == null) {
    System.err.println("gwt.xml missing from jar")
    result = false
  }
  if (jarFile.getEntry("it/testlib/client/TestLib.java") == null) {
    System.err.println("Java source missing from jar")
    result = false
  }
  if (jarFile.getEntry("it/testlib/client/TestLib.class") == null) {
    System.err.println("Compiled Java class missing from jar")
    result = false
  }
  if (jarFile.getEntry("it/testlib/super/it/testlib/client/Super.java") == null) {
    System.err.println("Java super-source missing from jar")
    result = false
  }
  if (jarFile.getEntry("it/testlib/super/it/testlib/client/Super.class") != null) {
    System.err.println("jar erroneously contains compiled Java super-source")
    result = false
  }
  if (jarFile.getEntry("it/testlib/client/AutoValue_Processed.java") == null) {
    System.err.println("Generated java source missing from jar")
    result = false
  }
  if (jarFile.getEntry("it/testlib/client/AutoValue_Processed.class") == null) {
    System.err.println("Compiled generated Java class missing from jar")
    result = false
  }

  File buildLogFile = new File(basedir, "build.log")
  if (!buildLogFile.exists() || buildLogFile.isDirectory()) {
    System.err.println("build.log file is missing or a directory.")
    result = false
  } else {
    String buildLog = buildLogFile.text
    if (buildLog.contains("build is platform dependent")) {
      System.err.println("Encoding is not set.")
      result = false
    }
    if (!buildLog.contains("Tests run: 3, Failures: 0, Errors: 0, Skipped: 0")) {
      System.err.println("build.log does not talk about running tests")
      result = false
    }
  }

  return result
} catch (Throwable t) {
  t.printStackTrace()
  return false
}

return true
