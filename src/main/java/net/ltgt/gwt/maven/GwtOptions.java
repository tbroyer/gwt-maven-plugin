package net.ltgt.gwt.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.maven.plugin.logging.Log;

public interface GwtOptions {
  enum LogLevel {
    ERROR,
    WARN,
    INFO,
    TRACE,
    DEBUG,
    SPAM,
    ;

    public static LogLevel getLogLevel(Log log) {
      LogLevel logLevel;
      if (log.isDebugEnabled()) {
        logLevel = DEBUG;
      } else if (log.isInfoEnabled()) {
        logLevel = INFO;
      } else if (log.isWarnEnabled()) {
        logLevel = WARN;
      } else {
        logLevel = ERROR;
      }
      return logLevel;
    }
  }

  enum Style {
    DETAILED,
    OBFUSCATED,
    PRETTY,
    ;
  }

  class CommandlineBuilder {
    public static List<String> buildArgs(Log log, GwtOptions options) {
      List<String> args = new ArrayList<>();
      args.add("-logLevel");
      args.add(getLogLevel(log, options.getLogLevel()));
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
      args.add("-style");
      args.add(options.getStyle().name());
      args.add("-localWorkers");
      args.add(getLocalWorkers(options.getLocalWorkers()));
      if (options.isDraftCompile()) {
        args.add("-draftCompile");
      } else {
        args.add("-optimize");
        args.add(getOptimize(options.getOptimize()));
      }
      if (options.getSourceLevel() != null) {
        args.add("-sourceLevel");
        args.add(options.getSourceLevel());
      }
      return args;
    }

    private static String getLogLevel(Log log, LogLevel logLevel) {
      return (logLevel == null ? LogLevel.getLogLevel(log) : logLevel).name();
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

    private static String getOptimize(int optimize) {
      if (optimize < 0) {
        optimize = 0;
      } else if (optimize > 9) {
        optimize = 9;
      }
      return String.valueOf(optimize);
    }
  }

  LogLevel getLogLevel();

  Style getStyle();

  int getOptimize();

  File getWarDir();

  File getWorkDir();

  File getDeployDir();

  @Nullable File getExtraDir();

  boolean isDraftCompile();

  String getLocalWorkers();

  @Nullable String getSourceLevel();
}
