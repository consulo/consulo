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
package com.intellij.openapi.util;

import consulo.disposer.Disposable;
import consulo.annotation.DeprecationInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Deprecated
@DeprecationInfo("Use consulo.disposer.Disposer")
public class Disposer {
  @Nonnull
  public static Disposable newDisposable() {
    return newDisposable(null);
  }

  @Nonnull
  public static Disposable newDisposable(@Nullable final String debugName) {
    return new Disposable() {
      @Override
      public void dispose() {
      }

      @Override
      public String toString() {
        return debugName == null ? super.toString() : debugName;
      }
    };
  }

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child) {
    register(parent, child, null);
  }

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child, @Nullable final String key) {
    consulo.disposer.Disposer.register(parent, child, key);
  }

  public static boolean isDisposed(@Nonnull Disposable disposable) {
    return consulo.disposer.Disposer.isDisposed(disposable);
  }

  public static Throwable getDisposalTrace(@Nonnull Disposable disposable) {
    return consulo.disposer.Disposer.getDisposalTrace(disposable);
  }

  public static boolean isDisposing(@Nonnull Disposable disposable) {
    return consulo.disposer.Disposer.isDisposing(disposable);
  }

  public static void dispose(@Nonnull Disposable disposable) {
    consulo.disposer.Disposer.dispose(disposable);
  }

  public static void dispose(@Nonnull Disposable disposable, boolean processUnregistered) {
    consulo.disposer.Disposer.dispose(disposable, processUnregistered);
  }

  public static boolean isDebugMode() {
    return consulo.disposer.Disposer.isDebugMode();
  }

  public static boolean setDebugMode(boolean mode) {
    return consulo.disposer.Disposer.setDebugMode(mode);
  }

  public static void assertIsEmpty() {
    consulo.disposer.Disposer.assertIsEmpty();
  }
}
