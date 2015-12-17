import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.codehaus.plexus.util.*;

try {
  File target = new File(basedir, "target");
  if (!target.isDirectory()) {
    System.err.println("target file is missing or not a directory.");
    return false;
  }

  File moduleFile = new File(target, "classes/it/test/Test.gwt.xml");
  if (!moduleFile.isFile()) {
    System.err.println("it/test/Test.gwt.xml module is missing or is not a file.");
    return false;
  }
  String module = FileUtils.fileRead(moduleFile);
  if (module.contains("shared")) {
    System.err.println("GWT module has generated sources whereas it contained sources.");
    return false;
  }

  File war = new File(target, "gwt-application-1.0.war");
  if (!war.isFile()) {
    System.err.println("war file is missing or a not a file.");
    return false;
  }

  JarFile jarFile = new JarFile(war);
  Enumeration entries = jarFile.entries();

  boolean seenNocacheJs = false;
  boolean seenWebInf = false;
  while (entries.hasMoreElements()) {
    JarEntry entry = entries.nextElement();
    String name = entry.getName();
    if (name.equals("test/test.nocache.js")) {
      seenNocacheJs = true;
    } else if (name.startsWith("WEB-INF/")) {
      seenWebInf = true;
    }
  }
  if (!seenNocacheJs) {
    System.err.println("test/test.nocache.js missing from war");
    return false;
  }
  if (seenWebInf) {
    System.err.println("war file erroneously contains a WEB-INF/");
    return false;
  }

  File buildLogFile = new File(basedir, "build.log");
  if (!buildLogFile.exists() || buildLogFile.isDirectory()) {
    System.err.println("build.log file is missing or a directory.");
    return false;
  }
  String buildLog = FileUtils.fileRead(buildLogFile);
  if (buildLog.contains("build is platform dependent")) {
    System.err.println("Encoding is not set.");
    return false;
  }
  if (!buildLog.contains("Tests run: 3, Failures: 0, Errors: 0, Skipped: 0")) {
    System.err.println("build.log does not talk about running tests");
    return false;
  }
} catch (Throwable t) {
  t.printStackTrace();
  return false;
}

return true;