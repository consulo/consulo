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
import com.intellij.util.TimeoutUtil;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author VISTALL
 * @since 2018-01-06
 */
public class WriteThread extends Thread implements Disposable {
  private static class CallInfo {
    private ThrowableComputable myComputable;
    private AsyncResult myResult;
    private Class myCallClass;

    private CallInfo(ThrowableComputable computable, AsyncResult result, Class callClass) {
      myComputable = computable;
      myResult = result;
      myCallClass = callClass;
    }
  }

  private final Deque<CallInfo> myQueue = new ConcurrentLinkedDeque<>();
  private final BaseApplicationWithOwnWriteThread myApplication;

  private boolean myStop;

  public WriteThread(@Nullable BaseApplicationWithOwnWriteThread application) {
    super("Consulo Write Thread");
    myApplication = application;

    setPriority(MAX_PRIORITY);

    start();
  }

  public <T> void push(ThrowableComputable<T, Throwable> computable, AsyncResult<T> result, Class caller) {
    if(myApplication.isWriteThread()) {
      runImpl(caller, computable, result);
      return;
    }
    myQueue.addLast(new CallInfo(computable, result, caller));
  }

  @Override
  public void run() {
    while (!myStop) {
      try {
        CallInfo first = myQueue.pollFirst();
        if (first != null) {
          runImpl(first.myCallClass, first.myComputable, first.myResult);
        }
      }
      finally {
        TimeoutUtil.sleep(100);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void runImpl(Class caller, ThrowableComputable computable, AsyncResult asyncResult) {
    try {
      Object compute;
      //noinspection RequiredXAction
      try(AccessToken ignored = myApplication.acquireWriteActionLock(caller)) {
        compute = computable.compute();
      }

      myApplication.executeOnPooledThread(() -> asyncResult.setDone(compute));
    }
    catch (Throwable throwable) {
      throwable.printStackTrace();
      myApplication.executeOnPooledThread(() -> asyncResult.rejectWithThrowable(throwable));
    }
  }

  @Override
  public void dispose() {
    myStop = true;
  }
}
