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
package consulo.util.lang;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class ExceptionUtil {
  private ExceptionUtil() {
  }

  public static Throwable getRootCause(Throwable e) {
    while (true) {
      if (e.getCause() == null) return e;
      e = e.getCause();
    }
  }

  @SuppressWarnings("unchecked")
  public static @Nullable <T> T findCause(Throwable e, Class<T> clazz) {
    @Nullable Throwable cause = e;
    while (cause != null && !clazz.isInstance(cause)) {
      cause = cause.getCause();
    }
    return (T) cause;
  }

  public static boolean causedBy(Throwable e, Class clazz) {
    return findCause(e, clazz) != null;
  }

  public static Throwable makeStackTraceRelative(Throwable th, Throwable relativeTo) {
    StackTraceElement[] trace = th.getStackTrace();
    StackTraceElement[] rootTrace = relativeTo.getStackTrace();
    for (int i = 0, len = Math.min(trace.length, rootTrace.length); i < len; i++) {
      if (trace[trace.length - i - 1].equals(rootTrace[rootTrace.length - i - 1])) continue;
      int newDepth = trace.length - i;
      th.setStackTrace(Arrays.asList(trace).subList(0, newDepth).toArray(new StackTraceElement[newDepth]));
      break;
    }
    return th;
  }

  public static String currentStackTrace() {
    return getThrowableText(new Throwable());
  }

  public static String getThrowableText(Throwable aThrowable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    aThrowable.printStackTrace(writer);
    return stringWriter.getBuffer().toString();
  }

  public static String getThrowableText(Throwable aThrowable, String stackFrameSkipPattern) {
    final String prefix = "\tat ";
    final String prefixProxy = prefix + "$Proxy";
    final String prefixRemoteUtil = prefix + "consulo.util.rmi.RemoteUtil";
    final String skipPattern = prefix + stackFrameSkipPattern;

    final StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter) {
      private boolean skipping;

      @Override
      public void println(String x) {
        boolean curSkipping = skipping;
        if (x != null) {
          if (!skipping && x.startsWith(skipPattern)) curSkipping = true;
          else if (skipping && !x.startsWith(prefix)) curSkipping = false;
          if (curSkipping && !skipping) {
            super.println("\tin " + stripPackage(x, skipPattern.length()));
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

  public static @Nullable String getMessage(Throwable e) {
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

  private static String extractMessage(String result, String errorPattern) {
    if (result.lastIndexOf(errorPattern) >= 0) {
      result = result.substring(result.lastIndexOf(errorPattern) + errorPattern.length());
    }
    return result;
  }

  public static void rethrowUnchecked(@Nullable Throwable t) {
    if (t != null) {
      if (t instanceof Error e) {
        throw e;
      }
      if (t instanceof RuntimeException re) {
        throw re;
      }
    }
  }

  @Contract("!null -> fail")
  public static void rethrowAll(@Nullable Throwable t) throws Exception {
    if (t != null) {
      rethrowUnchecked(t);
      throw (Exception) t;
    }
  }

  public static void rethrow(@Nullable Throwable throwable) {
    if (throwable instanceof Error e) {
      throw e;
    }
    else if (throwable instanceof RuntimeException re) {
      throw re;
    }
    else {
      throw new RuntimeException(throwable);
    }
  }

  public static void rethrowAllAsUnchecked(@Nullable Throwable t) {
    if (t != null) {
      rethrowUnchecked(t);
      throw new RuntimeException(t);
    }
  }

  public static String getNonEmptyMessage(Throwable t, String defaultMessage) {
    String message = t.getMessage();
    return !StringUtil.isEmptyOrSpaces(message) ? message : defaultMessage;
  }
}