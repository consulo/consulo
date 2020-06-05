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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * kotlin coroutine stub
 */
class Job {
  private static final int PENDING = 0;
  private static final int CANCEL = 1;

  private int myState = PENDING;

  private List<Consumer<Throwable>> myOnComplete = new CopyOnWriteArrayList<>();

  public boolean isCompleted() {
    return myState != PENDING;
  }

  @Nonnull
  public Disposable invokeOnCompletion(Consumer<Throwable> handler) {
    return invokeOnCompletion(false, handler);
  }

  @Nonnull
  public Disposable invokeOnCompletion(boolean onCancelling, Consumer<Throwable> handler) {
    if (myState != PENDING) {

      handler.accept(new Exception("canceled"));

      return () -> {
      };
    }

    // fixme [vistall] no sence in onCancelling

    myOnComplete.add(handler);
    return () -> myOnComplete.remove(handler);
  }

  public void cancel() {
    if (myState != PENDING) {
      return;
    }

    myState = CANCEL;

    Exception throwable = new Exception("cancel");
    for (Consumer<Throwable> consumer : myOnComplete) {
      consumer.accept(throwable);
    }
  }
}
