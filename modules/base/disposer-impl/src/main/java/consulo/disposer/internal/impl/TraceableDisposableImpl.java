/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.disposer.internal.impl;

import consulo.disposer.TraceableDisposable;
import consulo.disposer.internal.impl.objectTree.ThrowableInterner;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Traces creation and disposal by storing corresponding stacktraces.
 * In constructor it saves creation stacktrace
 * In kill() it saves disposal stacktrace
 */
public class TraceableDisposableImpl implements TraceableDisposable {
  private final Throwable CREATE_TRACE;
  private Throwable KILL_TRACE;

  public TraceableDisposableImpl(boolean debug) {
    CREATE_TRACE = debug ? ThrowableInterner.intern(new Throwable()) : null;
  }

  @Override
  public void kill(@Nullable String msg) {
    if (CREATE_TRACE != null) {
      KILL_TRACE = ThrowableInterner.intern(new Throwable(msg));
    }
  }

  @Override
  public void killExceptionally(@Nonnull Throwable throwable) {
    if (CREATE_TRACE != null) {
      KILL_TRACE = throwable;
    }
  }

  /**
   * Call when object is not disposed while it should
   */
  @Override
  public void throwObjectNotDisposedError(@Nonnull String msg) {
    throw new ObjectNotDisposedException(msg);
  }

  private class ObjectNotDisposedException extends AbstractDisposalException {

    ObjectNotDisposedException(@Nullable String msg) {
      super(msg);
    }


    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public void printStackTrace(PrintWriter s) {
      List<StackTraceElement> stack = new ArrayList<StackTraceElement>(Arrays.asList(CREATE_TRACE.getStackTrace()));
      stack.remove(0); // this line is useless it stack
      s.write(ObjectNotDisposedException.class.getCanonicalName() + ": See stack trace responsible for creation of unreleased object below \n\tat " + StringUtil.join(stack, "\n\tat "));
    }
  }

  /**
   * in case of "object not disposed" use {@link #throwObjectNotDisposedError(String)} instead
   */
  @Override
  public void throwDisposalError(String msg) throws RuntimeException {
    throw new DisposalException(msg);
  }

  private abstract class AbstractDisposalException extends RuntimeException {
    protected AbstractDisposalException(String message) {
      super(message);
    }

    @Override
    public void printStackTrace(@Nonnull PrintStream s) {
      //noinspection IOResourceOpenedButNotSafelyClosed
      PrintWriter writer = new PrintWriter(s);
      printStackTrace(writer);
      writer.flush();
    }
  }

  private class DisposalException extends AbstractDisposalException {
    private DisposalException(String message) {
      super(message);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public void printStackTrace(PrintWriter s) {
      if (CREATE_TRACE != null) {
        s.println("--------------Creation trace: ");
        CREATE_TRACE.printStackTrace(s);
      }
      if (KILL_TRACE != null) {
        s.println("--------------Kill trace: ");
        KILL_TRACE.printStackTrace(s);
      }
      s.println("-------------Own trace:");
      super.printStackTrace(s);
    }
  }

  @Override
  @Nonnull
  public String getStackTrace() {
    StringWriter out = new StringWriter();
    new DisposalException("").printStackTrace(new PrintWriter(out));
    return out.toString();
  }
}
