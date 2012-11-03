package net.ltgt.gwt.maven;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

import org.apache.maven.plugin.logging.Log;

public class MavenTreeLogger extends AbstractTreeLogger {

  private final Log log;
  private final String indent;

  public MavenTreeLogger(Log log) {
    this(log, "");
  }

  private MavenTreeLogger(Log log, String indent) {
    this.log = log;
    this.indent = indent;
    if (log.isDebugEnabled()) {
      setMaxDetail(DEBUG);
    } else if (log.isInfoEnabled()) {
      setMaxDetail(INFO);
    } else if (log.isWarnEnabled()) {
      setMaxDetail(WARN);
    } else {
      setMaxDetail(ERROR);
    }
  }

  @Override
  protected AbstractTreeLogger doBranch() {
    return new MavenTreeLogger(log, indent + "   ");
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted, Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    // We want to display the branch even if its level is not enabled (that's the whole point of doCommitBranch),
    if (!isLoggable(type)) {
      type = getMaxDetail();
    }
    doLog(childBeingCommitted.getBranchedIndex(), type, msg, caught, helpInfo);
  }

  @Override
  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    msg = indent + msg;
    if (caught instanceof UnableToCompleteException) {
      caught = null;
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
