// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.constraints;

import consulo.disposer.Disposable;
import com.intellij.openapi.util.NotNullLazyValue;

import java.util.Objects;

/**
 * DisposableExpiration isolates interactions with a Disposable and the Disposer, using
 * an expirable supervisor job that gets cancelled whenever the Disposable is disposed.
 * <p>
 * The DisposableExpiration itself is a lightweight thing w.r.t. creating it until it's supervisor Job
 * is really used, because registering a child Disposable within the Disposer tree happens lazily.
 */
public class DisposableExpiration extends AbstractExpiration {

  private final Disposable myDisposable;
  private NotNullLazyValue<Job> job;

  public DisposableExpiration(Disposable disposable) {
    myDisposable = disposable;
    job = NotNullLazyValue.createValue(() -> {
      Job job = new Job();
      ExpirationUtil.cancelJobOnDisposal(disposable, job, true);
      return job;
    });
  }

  @Override
  protected Job getJob() {
    return job.getValue();
  }

  @Override
  public boolean isExpired() {
    return getJob().isCompleted() && ExpirationUtil.isDisposed(myDisposable);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DisposableExpiration that = (DisposableExpiration)o;
    return Objects.equals(myDisposable, that.myDisposable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myDisposable);
  }
}
