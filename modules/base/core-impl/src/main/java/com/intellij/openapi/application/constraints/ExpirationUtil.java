/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.openapi.application.constraints;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.WeakReferenceDisposableWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * from kotlin
 */
public class ExpirationUtil {
  @Nullable
  static Expiration composeExpiration(Collection<? extends Expiration> expirations) {
    int size = expirations.size();
    if (size == 0) {
      return null;
    }
    else if (size == 1) {
      return ContainerUtil.getFirstItem(expirations);
    }
    else {
      AsyncPromise<Void> job = new AsyncPromise<>();

      for (Expiration expiration : expirations) {
        cancelJobOnExpiration(expiration, job);
      }

      return new JobExpiration(job);
    }
  }

  private static Expiration.Handle cancelJobOnExpiration(Expiration expiration, AsyncPromise<Void> job) {
    Expiration.Handle handle = expiration.invokeOnExpiration(() -> job.setError("rejected"));
    job.onSuccess(aVoid -> handle.unregisterHandler());
    return handle;
  }

  public static boolean isDisposed(Disposable disposable) {
    return Disposer.isDisposed(disposable);
  }

  private static boolean isDisposing(Disposable disposable) {
    return Disposer.isDisposing(disposable);
  }

  private static boolean tryRegisterDisposable(Disposable parent, Disposable child) {
    if (!isDisposed(parent) && !isDisposing(parent)) {
      try {
        Disposer.register(parent, child);
      }
      catch (IncorrectOperationException e) { // Sorry but Disposer.register() is inherently thread-unsafe
      }
    }

    return false;
  }

  public static AutoCloseable cancelJobOnDisposal(Disposable thisDisposable, AsyncPromise<Void> job, boolean weaklyReferencedJob) {
    ExpirableConstrainedExecution.RunOnce runOnce = new ExpirableConstrainedExecution.RunOnce();

    Disposable child = () -> {
      job.setError("rejected");
    };

    Disposable childRef = !weaklyReferencedJob ? child : new WeakReferenceDisposableWrapper(child);

    if (!tryRegisterDisposable(thisDisposable, childRef)) {
      Disposer.dispose(childRef);  // runs disposableBlock()
      return () -> {
      };
    }
    else {
      Runnable completionHandler = new Runnable() {
        @SuppressWarnings("unused")
        Disposable hardRefToChild = child; // transitive: job -> completionHandler -> child

        @Override
        public void run() {
          runOnce.invoke(() -> {
            Disposer.dispose(childRef);
          });
        }
      };

      Promise<Void> jobCompletionUnregisteringHandle = job.onSuccess(aVoid -> completionHandler.run());

      return () -> {
        // fixme [VISTALL] we need this??
        // jobCompletionUnregisteringHandle.dispose();
        completionHandler.run();
      };
    }
  }
}
