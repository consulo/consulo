/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Use this reporter for the cases when exception occurs within coverage engine
 */
public class ErrorReporter {
  private final static String ERROR_FILE = "coverage-error.log";
  private final static SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
  private static String basePath;

  public static synchronized void reportError(final String message) {
    PrintStream os = null;
    try {
      os = getErrorLogStream();
      StringBuffer buf = prepareMessage(message);

      System.err.println(buf.toString());
      os.println(buf.toString());
    } catch (IOException e) {
      System.err.println("Failed to write to error log file: " + e.toString());
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  public static synchronized void reportError(final String message, Throwable t) {
    PrintStream os = null;
    try {
      os = getErrorLogStream();
      StringBuffer buf = prepareMessage(message);

      System.err.println(buf.toString() + ": " + t.toString());
      os.println(buf.toString());

      t.printStackTrace(os);
    } catch (IOException e) {
      System.err.println("Failed to write to error log file: " + e.toString());
      System.err.println("Initial stack trace: " + t.toString());
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  public static synchronized void logError(final String message) {
    PrintStream os = null;
    try {
      os = getErrorLogStream();
      StringBuffer buf = prepareMessage(message);
      os.println(buf.toString());
    } catch (IOException e) {
      System.err.println("Failed to write to error log file: " + e.toString());
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  private static PrintStream getErrorLogStream() throws FileNotFoundException {
    return new PrintStream(new FileOutputStream(basePath != null ? new File(basePath, ERROR_FILE) : new File(ERROR_FILE), true));
  }

  private static StringBuffer prepareMessage(final String message) {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    buf.append(myDateFormat.format(new Date()));
    buf.append("] (Coverage): ");
    buf.append(message);
    return buf;
  }

  public static void setBasePath(String basePath) {
    ErrorReporter.basePath = basePath;
  }
}
