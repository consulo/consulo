/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public interface UIAccess {
  /**
   * @return if current thread can access to ui write mode
   */
  static boolean isUIThread() {
    return UIInternal.get()._UIAccess_isUIThread();
  }

  @RequiredUIAccess
  @Nonnull
  @Deprecated
  static UIAccess get() {
    return current();
  }

  /**
   * If we inside ui thread, we can get ui access
   *
   * @return ui access - or throw exception
   */
  @RequiredUIAccess
  @Nonnull
  static UIAccess current() {
    assertIsUIThread();

    return UIInternal.get()._UIAccess_get();
  }

  @RequiredUIAccess
  static void assertIsUIThread() {
    if (!isUIThread()) {
      throw new IllegalArgumentException("Call must be wrapped inside UI thread. Current thread: " + Thread.currentThread().getName());
    }
  }

  static void assetIsNotUIThread() {
    if(isUIThread()) {
      throw new IllegalArgumentException("Call must be wrapped outside UI thread. Current thread: " + Thread.currentThread().getName());
    }
  }

  @RequiredUIAccess
  default int getEventCount() {
    assertIsUIThread();
    return -1;
  }

  boolean isValid();

  @Nonnull
  default AsyncResult<Void> give(@RequiredUIAccess @Nonnull Runnable runnable) {
    return give(() -> {
      runnable.run();
      return null;
    });
  }

  @Nonnull
  <T> AsyncResult<T> give(@RequiredUIAccess @Nonnull Supplier<T> supplier);

  default void giveAndWait(@RequiredUIAccess @Nonnull Runnable runnable) {
    give(runnable).getResultSync();
  }

  @SuppressWarnings("unchecked")
  default <T> T giveAndWaitIfNeed(@RequiredUIAccess @Nonnull Supplier<T> supplier) {
    Object[] value = new Object[1];
    giveAndWaitIfNeed((Runnable)() -> value[0] = supplier.get());
    return (T)value[0];
  }

  default void giveIfNeed(@RequiredUIAccess @Nonnull Runnable runnable) {
    if (isUIThread()) {
      runnable.run();
    }
    else {
      give(runnable);
    }
  }

  default void giveAndWaitIfNeed(@RequiredUIAccess @Nonnull Runnable runnable) {
    if (isUIThread()) {
      runnable.run();
    }
    else {
      giveAndWait(runnable);
    }
  }

  default boolean isHeadless() {
    return false;
  }
}