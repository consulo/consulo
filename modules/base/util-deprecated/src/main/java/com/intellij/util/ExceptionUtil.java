/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.util;

import consulo.logging.Logger;
import com.intellij.openapi.util.text.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class ExceptionUtil {
  private ExceptionUtil() { }

  @Nonnull
  public static Throwable getRootCause(@Nonnull Throwable e) {
    while (true) {
      if (e.getCause() == null) return e;
      e = e.getCause();
    }
  }

  public static <T> T findCause(Throwable e, Class<T> klass) {
    while (e != null && !klass.isInstance(e)) {
      e = e.getCause();
    }
    @SuppressWarnings("unchecked") T t = (T)e;
    return t;
  }

  public static boolean causedBy(Throwable e, Class klass) {
    return findCause(e, klass) != null;
  }

  @Nonnull
  public static Throwable makeStackTraceRelative(@Nonnull Throwable th, @Nonnull Throwable relativeTo) {
    StackTraceElement[] trace = th.getStackTrace();
    StackTraceElement[] rootTrace = relativeTo.getStackTrace();
    for (int i=0, len = Math.min(trace.length, rootTrace.length); i < len; i++) {
      if (trace[trace.length - i - 1].equals(rootTrace[rootTrace.length - i - 1])) continue;
      int newDepth = trace.length - i;
      th.setStackTrace(Arrays.asList(trace).subList(0, newDepth).toArray(new StackTraceElement[newDepth]));
      break;
    }
    return th;
  }

  @Nonnull
  public static String currentStackTrace() {
    return getThrowableText(new Throwable());
  }

  @Nonnull
  public static String getThrowableText(@Nonnull Throwable aThrowable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    aThrowable.printStackTrace(writer);
    return stringWriter.getBuffer().toString();
  }

  @Nonnull
  public static String getThrowableText(@Nonnull Throwable aThrowable, @Nonnull String stackFrameSkipPattern) {
    final String prefix = "\tat ";
    final String prefixProxy = prefix + "$Proxy";
    final String prefixRemoteUtil = prefix + "consulo.util.rmi.RemoteUtil";
    final String skipPattern = prefix + stackFrameSkipPattern;

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter writer = new PrintWriter(stringWriter) {
      private boolean skipping;
      @Override
      public void println(final String x) {
        boolean curSkipping = skipping;
        if (x != null) {
          if (!skipping && x.startsWith(skipPattern)) curSkipping = true;
          else if (skipping && !x.startsWith(prefix)) curSkipping = false;
          if (curSkipping && !skipping) {
            super.println("\tin "+ stripPackage(x, skipPattern.length()));
          }
          skipping = curSkipping;
          if (skipping) {
            skipping = !x.startsWith(prefixRemoteUtil);
            return;
          }
          if (x.startsWith(prefixProxy)) return;
          super.println(x);
        }
      }
    };
    aThrowable.printStackTrace(writer);
    return stringWriter.getBuffer().toString();
  }

  private static String stripPackage(String x, int offset) {
    int idx = offset;
    while (idx > 0 && idx < x.length() && !Character.isUpperCase(x.charAt(idx))) {
      idx = x.indexOf('.', idx) + 1;
    }
    return x.substring(Math.max(idx, offset));
  }

  @Nonnull
  public static String getUserStackTrace(@Nonnull Throwable aThrowable, Logger logger) {
    final String result = getThrowableText(aThrowable, "com.intellij.");
    if (!result.contains("\n\tat")) {
      // no stack frames found
      logger.error(aThrowable);
    }
    return result;
  }

  @Nullable
  public static String getMessage(@Nonnull Throwable e) {
    String result = e.getMessage();
    String exceptionPattern = "Exception: ";
    String errorPattern = "Error: ";

    while ((result == null || result.contains(exceptionPattern) || result.contains(errorPattern)) && e.getCause() != null) {
      e = e.getCause();
      result = e.getMessage();
    }

    if (result != null) {
      result = extractMessage(result, exceptionPattern);
      result = extractMessage(result, errorPattern);
    }

    return result;
  }

  @Nonnull
  private static String extractMessage(@Nonnull String result, @Nonnull String errorPattern) {
    if (result.lastIndexOf(errorPattern) >= 0) {
      result = result.substring(result.lastIndexOf(errorPattern) + errorPattern.length());
    }
    return result;
  }

  public static void rethrowUnchecked(@Nullable Throwable t) {
    consulo.util.lang.ExceptionUtil.rethrowUnchecked(t);
  }

  public static void rethrowAll(@Nullable Throwable t) throws Exception {
    consulo.util.lang.ExceptionUtil.rethrowAll(t);
  }

  public static void rethrow(@Nullable Throwable throwable) {
    consulo.util.lang.ExceptionUtil.rethrow(throwable);
  }

  public static void rethrowAllAsUnchecked(@Nullable Throwable t) {
    consulo.util.lang.ExceptionUtil.rethrowAllAsUnchecked(t);
  }

  @Nonnull
  public static String getNonEmptyMessage(@Nonnull Throwable t, @Nonnull String defaultMessage) {
    String message = t.getMessage();
    return !StringUtil.isEmptyOrSpaces(message) ? message : defaultMessage;
  }
}