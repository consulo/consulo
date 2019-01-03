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

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.impl.ReadMostlyRWLock;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import consulo.annotations.DeprecationInfo;
import consulo.application.ApplicationWithOwnWriteThread;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-12
 */
public abstract class BaseApplicationWithOwnWriteThread extends BaseApplication implements ApplicationWithOwnWriteThread {
  private static final Logger LOGGER = Logger.getInstance(BaseApplicationWithOwnWriteThread.class);

  private final WriteThread myWriteThread;

  public BaseApplicationWithOwnWriteThread(@Nonnull Ref<? extends StartupProgress> splashRef) {
    super(splashRef);

    myWriteThread = new WriteThread(this);
    myLock = new ReadMostlyRWLock(myWriteThread);

    Disposer.register(myLastDisposable, myWriteThread);
  }

  /**
   * Returns lock used for write operations, should be closed in finally block
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use runWriteAction(Runnable)")
  @RequiredUIAccess
  public AccessToken acquireWriteActionLock(@Nonnull Class marker) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public AccessToken acquireWriteActionLockInternal(@Nonnull Class clazz) {
    return new WriteAccessToken(clazz);
  }

  @Override
  @Nonnull
  public <T> AsyncResult<T> pushWriteAction(@Nonnull Class<?> caller, @Nonnull ThrowableComputable<T, Throwable> computable) {
    AsyncResult<T> asyncResult = new AsyncResult<T>() {
      @Nullable
      @Override
      public T getResultSync(long msTimeout) {
        if (isReadAccessAllowed() && !isWriteAccessAllowed() && !isDispatchThread()) {
          throw new IllegalStateException("Can't block waiting thread inside read lock");
        }

        if(isWriteAccessAllowed()) {
          throw new IllegalStateException("Self blocking write thread call");
        }

        if (isDispatchThread()) {
          LOGGER.warn(new Exception("UI thread is not good idea as waiting thread"));
        }
        return super.getResultSync(msTimeout);
      }
    };
    myWriteThread.push(computable, asyncResult, caller);
    return asyncResult;
  }

  @Override
  public boolean isReadAccessAllowed() {
    return isWriteAccessAllowed() || myLock.isReadLockedByThisThread() || isDispatchThread();
  }

  @Override
  public boolean isWriteAccessAllowed() {
    return Thread.currentThread() == myWriteThread;
  }
}
