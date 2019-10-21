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
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.constraints.ConstrainedExecution;
import com.intellij.openapi.application.constraints.ExpirableConstrainedExecution;
import com.intellij.openapi.application.constraints.Expiration;
import org.jetbrains.concurrency.CancellablePromise;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * from kotlin
 */
public abstract class BaseExpirableExecutorMixinImpl<E extends BaseExpirableExecutorMixinImpl<E>> extends ExpirableConstrainedExecution<E> implements ConstrainedExecution<E> {
  private final Executor executor;

  protected BaseExpirableExecutorMixinImpl(ConstrainedExecution.ContextConstraint[] constraints, BooleanSupplier[] cancellationConditions, Set<? extends Expiration> expirableHandles, Executor executor) {
    super(constraints, cancellationConditions, expirableHandles);
    this.executor = executor;
  }

  @Override
  public void scheduleWithinConstraints(Runnable runnable, @Nullable BooleanSupplier condition) {
    executor.execute(() -> super.scheduleWithinConstraints(runnable, condition));
  }

  public void execute(Runnable runnable) {
    asExecutor().execute(runnable);
  }

  public CancellablePromise<?> submit(Runnable runnable) {
    return asExecutor().submit(runnable);
  }

  public <T> CancellablePromise<T> submit(Callable<T> task) {
    return asExecutor().submit(task);
  }
}
