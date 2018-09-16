/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.idea;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import consulo.application.ApplicationProperties;
import consulo.util.logging.LoggerFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.xml.DOMConfigurator;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.StringReader;

public class Log4JLoggerFactory implements LoggerFactory {
  private static final String SYSTEM_MACRO = "$SYSTEM_DIR$";
  private static final String APPLICATION_MACRO = "$APPLICATION_DIR$";
  private static final String LOG_DIR_MACRO = "$LOG_DIR$";

  public Log4JLoggerFactory() {
    // FIXME [VISTALL] idk need we this code or not, since we don't use log4j as logger

    // avoiding "log4j:WARN No appenders could be found"
    System.setProperty("log4j.defaultInitOverride", "true");
    try {
      org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
      if (!root.getAllAppenders().hasMoreElements()) {
        root.setLevel(Level.WARN);
        root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.DEFAULT_CONVERSION_PATTERN)));
      }
    }
    catch (Throwable e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }

    init();
  }

  @Nonnull
  @Override
  public synchronized Logger getLoggerInstance(String name) {
    return new Log4JLogger(org.apache.log4j.Logger.getLogger(name));
  }

  @Override
  public int getPriority() {
    return DEFAULT_PRIORITY;
  }

  @Override
  public void shutdown() {

  }

  private void init() {
    try {
      System.setProperty("log4j.defaultInitOverride", "true");

      String text = FileUtil.loadTextAndClose(Log4JLoggerFactory.class.getResourceAsStream("/log4j.xml"));
      text = StringUtil.replace(text, SYSTEM_MACRO, StringUtil.replace(PathManager.getSystemPath(), "\\", "\\\\"));
      text = StringUtil.replace(text, APPLICATION_MACRO, StringUtil.replace(PathManager.getHomePath(), "\\", "\\\\"));
      text = StringUtil.replace(text, LOG_DIR_MACRO, StringUtil.replace(PathManager.getLogPath(), "\\", "\\\\"));

      File file = new File(PathManager.getLogPath());
      if (!file.mkdirs() && !file.exists()) {
        System.err.println("Cannot create log directory: " + file);
      }

      new DOMConfigurator().doConfigure(new StringReader(text), LogManager.getLoggerRepository());

      if(Boolean.getBoolean(ApplicationProperties.CONSULO_MAVEN_CONSOLE_LOG)) {
        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();

        ConsoleAppender consoleAppender = new ConsoleAppender(new PatternLayout("[%7r] %6p - %30.30c - %m \n"), ConsoleAppender.SYSTEM_OUT);
        consoleAppender.setThreshold(Level.INFO);
        rootLogger.addAppender(consoleAppender);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
