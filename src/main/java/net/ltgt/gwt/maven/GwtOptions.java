package net.ltgt.gwt.maven;

import java.io.File;
import java.nio.file.Path;
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
    public static List<String> buildArgs(Log log, Path workingDir, GwtOptions options) {
      List<String> args = new ArrayList<>();
      args.add("-logLevel");
      args.add(getLogLevel(log, options.getLogLevel()));
      args.add("-war");
      args.add(workingDir.relativize(options.getWarDir().toPath()).toString());
      args.add("-workDir");
      args.add(workingDir.relativize(options.getWorkDir().toPath()).toString());
      args.add("-deploy");
      args.add(workingDir.relativize(options.getDeployDir().toPath()).toString());
      if (options.getExtraDir() != null) {
        args.add("-extra");
        args.add(workingDir.relativize(options.getExtraDir().toPath()).toString());
      }
      args.add("-style");
      args.add(options.getStyle().name());
      args.add("-localWorkers");
      args.add(getLocalWorkers(log, options.getLocalWorkers()));
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

    private static String getLocalWorkers(Log log, int localWorkers) {
      if (localWorkers < 1) {
        localWorkers = Runtime.getRuntime().availableProcessors();
        if (log.isDebugEnabled()) {
          log.debug("Using " + localWorkers + " local workers");
        }
      }
      return String.valueOf(localWorkers);
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

  int getLocalWorkers();

  @Nullable String getSourceLevel();
}
