/*
 * Copyright 2013-2016 must-be.org
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

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public abstract class UIAccess {
  /**
   * @return if current thread can access to ui write mode
   */
  public static boolean isUIThread() {
    return _UIInternals.impl()._isUIThread();
  }

  /**
   * If we inside ui thread, we can get ui access
   *
   * @return ui access - or throw exception
   */
  @RequiredUIThread
  @NotNull
  public static UIAccess get() {
    assertIsUIThread();

    return _UIInternals.impl()._get();
  }

  @RequiredUIThread
  public static void assertIsUIThread() {
    if (!isUIThread()) {
      throw new IllegalArgumentException("Call must be wrapped inside UI thread");
    }
  }

  public abstract void give(@RequiredUIThread @NotNull Runnable runnable);
}
