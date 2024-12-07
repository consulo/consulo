/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt.hint;

import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @see #hide()
 */
public interface Hint {
  /**
   * @param parentComponent    defines coordinate system where hint will be shown.
   *                           Cannot be <code>null</code>.
   * @param x                  x coordinate of hint in parent coordinate system
   * @param y                  y coordinate of hint in parent coordinate system
   * @param focusBackComponent component which should get focus when the hint will
   * @param hintInfo
   */
  void show(@Nonnull JComponent parentComponent, int x, int y, JComponent focusBackComponent, @Nonnull HintHint hintInfo);

  /**
   * @return whether the hint is showing or not
   */
  boolean isVisible();

  /**
   * Hides current hint object.
   * <p/>
   * <b>Note:</b> this method is also used as a destruction callback, i.e. it performs necessary de-initialization when
   * current hint is not necessary to use anymore. Hence, it's <b>necessary</b> to call it from place where you definitely
   * now that current hint will not be used.
   */
  void hide();

  void addHintListener(@Nonnull HintListener listener);

  void removeHintListener(@Nonnull HintListener listener);

  void pack();

  void setLocation(@Nonnull RelativePoint point);
}