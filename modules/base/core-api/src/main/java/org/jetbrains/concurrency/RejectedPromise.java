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
package org.jetbrains.concurrency;

import com.intellij.util.Consumer;
import com.intellij.util.Function;
import javax.annotation.Nonnull;

@Deprecated
public class RejectedPromise<T> extends Promise<T> {
  private final Throwable error;

  public RejectedPromise(@Nonnull Throwable error) {
    this.error = error;
  }

  @Nonnull
  @Override
  public Promise<T> done(@Nonnull Consumer<T> done) {
    return this;
  }

  @Nonnull
  @Override
  public Promise<T> processed(@Nonnull AsyncPromise<T> fulfilled) {
    fulfilled.setError(error);
    return this;
  }

  @Nonnull
  @Override
  public Promise<T> rejected(@Nonnull Consumer<Throwable> rejected) {
    if (!AsyncPromise.isObsolete(rejected)) {
      rejected.consume(error);
    }
    return this;
  }

  @Override
  public RejectedPromise<T> processed(@Nonnull Consumer<T> processed) {
    processed.consume(null);
    return this;
  }

  @Nonnull
  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> then(@Nonnull Function<T, SUB_RESULT> done) {
    //noinspection unchecked
    return (Promise<SUB_RESULT>)this;
  }

  @Nonnull
  @Override
  public State getState() {
    return State.REJECTED;
  }

  @Override
  public void notify(@Nonnull AsyncPromise<T> child) {
    child.setError(error);
  }
}