/*
 * Copyright 2013-2018 consulo.io
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
package consulo.application.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.ExceptionUtil;
import consulo.application.internal.ApplicationWithOwnWriteThread;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.LockSupport;

/**
 * @author VISTALL
 * @since 2018-01-06
 */
public class WriteThread extends Thread implements Disposable {
  private static class CallInfo {
    private ThrowableComputable myComputable;
    private AsyncResult myResult;
    private Class myCallClass;
    private Exception myCreateTrace;

    private CallInfo(ThrowableComputable computable, AsyncResult result, Class callClass) {
      myComputable = computable;
      myResult = result;
      myCallClass = callClass;
      myCreateTrace = new Exception();
    }
  }

  private static final Logger LOG = Logger.getInstance(WriteThread.class);

  private final Deque<CallInfo> myQueue = new ConcurrentLinkedDeque<>();
  private final ApplicationWithOwnWriteThread myApplication;

  private boolean myStop;

  public WriteThread(@Nullable ApplicationWithOwnWriteThread application) {
    super("Consulo Write Thread");
    myApplication = application;

    setPriority(MAX_PRIORITY);

    start();
  }

  public <T> void push(ThrowableComputable<T, Throwable> computable, AsyncResult<T> result, Class caller) {
    myQueue.addLast(new CallInfo(computable, result, caller));

    LockSupport.unpark(this);
  }

  @Override
  public void run() {
    while (!myStop) {
      CallInfo task;
      while ((task = myQueue.pollFirst()) != null) {
        runImpl(task.myCallClass, task.myComputable, task.myResult, task.myCreateTrace);
      }

      LockSupport.park();
    }
  }

  @SuppressWarnings("unchecked")
  public void runImpl(@Nonnull Class caller, @Nonnull ThrowableComputable computable, @Nonnull AsyncResult asyncResult, @Nonnull Exception e) {
    long start = System.currentTimeMillis();
    try {
      Object compute;
      try (AccessToken ignored = myApplication.acquireWriteActionLockInternal(caller)) {
        compute = computable.compute();
      }

      asyncResult.setDone(compute);
    }
    catch (Throwable throwable) {
      String throwableText = ExceptionUtil.getThrowableText(throwable);

      LOG.warn("Exception trace: " + throwableText);
      LOG.warn("Call trace: " + ExceptionUtil.getThrowableText(e));

      asyncResult.rejectWithThrowable(throwable);
    }
    finally {
      long l = System.currentTimeMillis() - start;

      if (l > 1_000L) {
        LOG.warn("Long write operation. Time: " + l, e);
      }
    }
  }

  @Override
  public void dispose() {
    myStop = true;
    LockSupport.unpark(this);
  }
}