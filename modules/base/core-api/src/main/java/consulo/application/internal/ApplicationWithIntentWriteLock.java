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
package consulo.application.internal;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.ThrowableComputable;
import javax.annotation.Nonnull;

public interface ApplicationWithIntentWriteLock extends Application {
  /**
   * Acquires IW lock if it's not acquired by the current thread.
   *
   * @param invokedClassFqn fully qualified name of the class requiring the write-intent lock.
   */
  default void acquireWriteIntentLock(@Nonnull String invokedClassFqn) {
    throw new UnsupportedOperationException();
  }

  /**
   * Releases IW lock.
   */
  default void releaseWriteIntentLock() {
    throw new UnsupportedOperationException();
  }

  /**
   * Runs the specified action, releasing the write-intent lock if it is acquired at the moment of the call.
   * <p>
   * This method is used to implement higher-level API. Please do not use it directly.
   */
  default <T, E extends Throwable> T runUnlockingIntendedWrite(@Nonnull ThrowableComputable<T, E> action) throws E {
    return action.compute();
  }

  /**
   * Runs the specified action under the write-intent lock. Can be called from any thread. The action is executed immediately
   * if no write-intent action is currently running, or blocked until the currently running write-intent action completes.
   * <p>
   * This method is used to implement higher-level API. Please do not use it directly.
   * Use {@link #invokeLaterOnWriteThread}, {@link com.intellij.openapi.application.WriteThread} or
   * {@link com.intellij.openapi.application.AppUIExecutor#onWriteThread()} to run code under the write-intent lock asynchronously.
   *
   * @param action the action to run
   */
  default void runIntendedWriteActionOnCurrentThread(@Nonnull Runnable action) {
    action.run();
  }
}
