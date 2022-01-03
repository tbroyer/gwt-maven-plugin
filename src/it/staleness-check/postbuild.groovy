try {
  File buildLogFile = new File(basedir, "build.log")
  if (!buildLogFile.exists() || buildLogFile.isDirectory()) {
    System.err.println("build.log file is missing or a directory.")
    return false
  }

  String buildLog = buildLogFile.text
  if (!buildLog.contains("Compiling module it.test.Test")) {
    System.err.println("build.log does not talk about compiling GWT module")
    return false
  }
  if (!buildLog.contains("Compilation output seems uptodate. GWT compilation skipped.")) {
    System.err.println("build.log does not talk about skipping GWT compilation")
    return false
  }

  if (!new File(basedir, "target/gwt-application-1.0/test/test.nocache.js").exists()) {
    System.err.println("GWT module has not been compiled.")
    return false
  }

} catch (Throwable t) {
  t.printStackTrace()
  return false
}

return true
