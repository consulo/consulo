/*
 * Copyright 2013-2022 consulo.io
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
package consulo.application.ui.wm;

import consulo.ui.Window;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 24-Feb-22
 */
public interface FocusableFrame {
  @Nonnull
  Window getWindow();

  default JComponent getComponent() {
    throw new AbstractMethodError(getClass().getName() + " is not implemented");
  }

  default boolean isActive() {
    return Window.getActiveWindow() == getWindow();
  }

  /**
   * Try focus frame for user(and move at top). Many oses not allow focus from another process
   */
  default void activate() {
    // non-guaranteed action
  }
}
