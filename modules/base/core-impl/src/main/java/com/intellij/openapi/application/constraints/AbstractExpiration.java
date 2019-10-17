// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.constraints;

import org.jetbrains.concurrency.AsyncPromise;

/**
 * Expiration backed by a [Job] instance.
 * <p>
 * A Job is easier to interact with because of using homogeneous Job API when using it to setup
 * coroutine cancellation, and w.r.t. its lifecycle and memory management. Using it also has
 * performance considerations: two lock-free Job.invokeOnCompletion calls vs. multiple
 * synchronized Disposer calls per each launched coroutine.
 * <p>
 * from kotlin
 */
public abstract class AbstractExpiration implements Expiration {
  protected abstract AsyncPromise<Void> getJob();

  @Override
  public boolean isExpired() {
    return getJob().isDone();
  }

  @Override
  public Handle invokeOnExpiration(Runnable runnable) {
    getJob().onError(throwable -> {
      runnable.run();
    });
    return () -> {
      // fixme [vistall] we need do something??
      // job.dispose();
    };
  }
}
