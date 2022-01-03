try {
  File buildLogFile = new File(basedir, "build.log")
  if (!buildLogFile.exists() || buildLogFile.isDirectory()) {
    System.err.println("build.log file is missing or a directory.")
    return false
  }

  if (!buildLogFile.text.contains("GWT compilation is skipped")) {
    System.err.println("build.log does not talk about skipping GWT compilation")
    return false
  }

  if (new File(basedir, "target/gwt-application-1.0/test/test.nocache.js").exists()) {
    System.err.println("GWT module seems to have been compiled.")
    return false
  }

} catch (Throwable t) {
  t.printStackTrace()
  return false
}

return true
