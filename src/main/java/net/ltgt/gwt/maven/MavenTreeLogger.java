package net.ltgt.gwt.maven;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import org.apache.maven.plugin.logging.Log;

public class MavenTreeLogger extends AbstractTreeLogger {

  public static MavenTreeLogger newInstance(Log log, Type type) {
    Type mavenLogLevel = getLogLevel(log);

    MavenTreeLogger logger = new MavenTreeLogger(log, mavenLogLevel);

    logger.setMaxDetail(type == null ? mavenLogLevel: type);

    return logger;
  }

  public static Type getLogLevel(Log log) {
    Type mavenLogLevel;
    if (log.isDebugEnabled()) {
      mavenLogLevel = DEBUG;
    } else if (log.isInfoEnabled()) {
      mavenLogLevel = INFO;
    } else if (log.isWarnEnabled()) {
      mavenLogLevel = WARN;
    } else {
      mavenLogLevel = ERROR;
    }
    return mavenLogLevel;
  }

  private final Log log;
  private final Type mavenLogLevel;
  private final String indent;

  private MavenTreeLogger(Log log, Type mavenLogLevel) {
    this(log, mavenLogLevel, "");
  }

  private MavenTreeLogger(Log log, Type mavenLogLevel, String indent) {
    this.log = log;
    this.mavenLogLevel = mavenLogLevel;
    this.indent = indent;
  }

  @Override
  protected AbstractTreeLogger doBranch() {
    return new MavenTreeLogger(log, mavenLogLevel, indent + "   ");
  }

  public Type getMavenLogLevel() {
    return mavenLogLevel;
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted, Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    doLog(childBeingCommitted.getBranchedIndex(), type, msg, caught, helpInfo);
  }

  @Override
  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    msg = indent + msg;
    if (caught instanceof UnableToCompleteException) {
      caught = null;
    }
    // Adapt log level to what's loggable by the Maven Log.
    if (mavenLogLevel.isLowerPriorityThan(type)) {
      type = mavenLogLevel;
    }
    switch (type) {
      case ERROR:
        log.error(msg, caught);
        break;
      case WARN:
        log.warn(msg, caught);
        break;
      case TRACE:
      case DEBUG:
      case SPAM:
        log.debug(msg, caught);
        break;
      case INFO:
      default:
        log.info(msg, caught);
        break;
    }
  }
}
