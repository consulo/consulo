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

import consulo.disposer.Disposable;
import com.intellij.openapi.WeakReferenceDisposableWrapper;
import consulo.disposer.Disposer;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Consumer;

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
      Job job = new Job();

      for (Expiration expiration : expirations) {
        cancelJobOnExpiration(expiration, job);
      }

      return new JobExpiration(job);
    }
  }

  private static Expiration.Handle cancelJobOnExpiration(Expiration expiration, Job job) {
    Expiration.Handle registration = expiration.invokeOnExpiration(() -> {
      job.cancel();
    });

    job.invokeOnCompletion(throwable -> {
      registration.unregisterHandler();
    });
    return registration;
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
        return true;
      }
      catch (IncorrectOperationException e) { // Sorry but Disposer.register() is inherently thread-unsafe
      }
    }

    return false;
  }

  public static AutoCloseable cancelJobOnDisposal(Disposable thisDisposable, Job job, boolean weaklyReferencedJob) {
    ExpirableConstrainedExecution.RunOnce runOnce = new ExpirableConstrainedExecution.RunOnce();

    Disposable child = () -> {
      runOnce.invoke(() -> {
        job.cancel();
      });
    };

    Disposable childRef = !weaklyReferencedJob ? child : new WeakReferenceDisposableWrapper(child);

    if (!tryRegisterDisposable(thisDisposable, childRef)) {
      Disposer.dispose(childRef);  // runs disposableBlock()
      return () -> {
      };
    }
    else {
      Consumer<Throwable> completionHandler = new Consumer<Throwable>() {
        @SuppressWarnings("unused")
        Disposable hardRefToChild = child; // transitive: job -> completionHandler -> child

        @Override
        public void accept(Throwable t) {
          runOnce.invoke(() -> {
            Disposer.dispose(childRef);
          });
        }
      };

      Disposable jobCompletionUnregisteringHandle = job.invokeOnCompletion(completionHandler);

      return () -> {
        jobCompletionUnregisteringHandle.dispose();

        completionHandler.accept(null);
      };
    }
  }
}
