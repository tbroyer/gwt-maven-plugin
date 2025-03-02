package net.ltgt.gwt.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.jspecify.annotations.Nullable;

public interface GwtOptions {

  class CommandlineBuilder {
    public static List<String> buildArgs(Log log, GwtOptions options) {
      List<String> args = new ArrayList<>();
      if (options.getLogLevel() != null) {
        args.add("-logLevel");
        args.add(options.getLogLevel());
      }
      args.add("-war");
      args.add(options.getWarDir().getAbsolutePath());
      args.add("-workDir");
      args.add(options.getWorkDir().getAbsolutePath());
      args.add("-deploy");
      args.add(options.getDeployDir().getAbsolutePath());
      if (options.getExtraDir() != null) {
        args.add("-extra");
        args.add(options.getExtraDir().getAbsolutePath());
      }
      if (options.getStyle() != null) {
        args.add("-style");
        args.add(options.getStyle());
      }
      if (options.getLocalWorkers() != null) {
        args.add("-localWorkers");
        args.add(getLocalWorkers(options.getLocalWorkers()));
      }
      if (options.isDraftCompile()) {
        args.add("-draftCompile");
      } else if (options.getOptimize() != null) {
        args.add("-optimize");
        args.add(String.valueOf(options.getOptimize().intValue()));
      }
      if (options.getSourceLevel() != null) {
        args.add("-sourceLevel");
        args.add(options.getSourceLevel());
      }
      return args;
    }

    private static String getLocalWorkers(String localWorkers) {
      final int workers;
      // Use the same algorithm as org.apache.maven.cli.MavenCli
      if (localWorkers.contains("C")) {
        workers = (int) (Float.valueOf(localWorkers.replace("C", ""))
            * Runtime.getRuntime().availableProcessors());
      } else {
        workers = Integer.valueOf(localWorkers);
      }
      return String.valueOf(workers);
    }
  }

  @Nullable String getLogLevel();

  @Nullable String getStyle();

  @Nullable Integer getOptimize();

  File getWarDir();

  File getWorkDir();

  File getDeployDir();

  @Nullable File getExtraDir();

  boolean isDraftCompile();

  @Nullable String getLocalWorkers();

  @Nullable String getSourceLevel();
}
