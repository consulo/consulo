// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal;

import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.util.lang.ExceptionUtil;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports exceptions thrown from frequently called methods (e.g. {@link java.awt.Component#paint(Graphics)}),
 * so that instead of polluting the log with hundreds of {@link Logger#error(Throwable) LOG.errors} it prints the error message
 * and the stacktrace once in a while.
 */
public class FrequentErrorLogger {

  private static final int REPORT_EVERY_NUM = 1000;
  
  private final Map<Integer, Integer> ourReportedIssues = new ConcurrentHashMap<>(); // stacktrace hash -> number of reports
  
  private final Logger myLogger;

  
  public static FrequentErrorLogger newInstance(Logger logger) {
    return new FrequentErrorLogger(logger);
  }

  private FrequentErrorLogger(Logger logger) {
    myLogger = logger;
  }

  public void error(String message, Throwable t) {
    report(t, () -> myLogger.error(message, t));
  }

  public void error(String message, Throwable t, Attachment... attachments) {
    report(t, () -> myLogger.error(message, t, attachments));
  }

  public void info(String message, Throwable t) {
    report(t, () -> myLogger.info(message, t));
  }

  private void report(Throwable t, Runnable writeToLog) {
    int hash = ExceptionUtil.getThrowableText(t).hashCode();
    Integer reportedTimes = ourReportedIssues.get(hash);
    if (reportedTimes == null || reportedTimes > REPORT_EVERY_NUM) {
      writeToLog.run();
      ourReportedIssues.put(hash, 1);
    }
    else {
      ourReportedIssues.put(hash, reportedTimes + 1);
    }
  }
}
