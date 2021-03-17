/*
 * Copyright 2013-2020 consulo.io
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
package consulo.disposer;

import consulo.disposer.internal.DisposerInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Disposer {
  private static final DisposerInternal ourInternal = DisposerInternal.ourInstance;

  public static TraceableDisposable newTraceDisposable(boolean debug) {
    return ourInternal.newTraceDisposable(debug);
  }

  @Nullable
  public static Disposable get(@Nonnull String key) {
    return ourInternal.get(key);
  }

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child) {
    register(parent, child, null);
  }

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child, @Nullable final String key) {
    ourInternal.register(parent, child, key);
  }

  public static boolean isDisposed(@Nonnull Disposable disposable) {
    return ourInternal.isDisposed(disposable);
  }

  public static Throwable getDisposalTrace(@Nonnull Disposable disposable) {
    return ourInternal.getDisposalTrace(disposable);
  }

  public static boolean isDisposing(@Nonnull Disposable disposable) {
    return ourInternal.isDisposing(disposable);
  }

  public static void dispose(@Nonnull Disposable disposable) {
    dispose(disposable, true);
  }

  public static void dispose(@Nonnull Disposable disposable, boolean processUnregistered) {
    ourInternal.dispose(disposable, processUnregistered);
  }

  /**
   * @return object registered on {@code parentDisposable} which is equal to object, or {@code null} if not found
   */
  @Nullable
  public static <T extends Disposable> T findRegisteredObject(@Nonnull Disposable parentDisposable, @Nonnull T object) {
    return ourInternal.findRegisteredObject(parentDisposable, object);
  }

  public static boolean isDebugMode() {
    return ourInternal.isDebugMode();
  }

  public static boolean setDebugMode(boolean mode) {
    return ourInternal.setDebugMode(mode);
  }

  public static void assertIsEmpty() {
    ourInternal.assertIsEmpty();
  }
}
