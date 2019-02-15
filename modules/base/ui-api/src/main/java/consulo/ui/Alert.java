/*
 * Copyright 2013-2017 consulo.io
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

import com.intellij.openapi.util.AsyncResult;
import consulo.ui.util.TraverseUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 01-Oct-17
 */
public interface Alert<V> {
  int OK = 1000;
  int CANCEL = 1001;
  int APPLY = 1002;
  int YES = 1003;
  int NO = 1004;

  /**
   * @return new alert builder, with default title - and default type *info*
   */
  @Nonnull
  static <L> Alert<L> create() {
    return UIInternal.get()._Alerts_create();
  }

  @Nonnull
  Alert<V> remember(@Nonnull AlertValueRemember<V> remember);

  @Nonnull
  Alert<V> asWarning();

  @Nonnull
  Alert<V> asError();

  @Nonnull
  Alert<V> asQuestion();

  @Nonnull
  default Alert<V> button(int buttonId, @Nonnull V simpleValue) {
    return button(buttonId, () -> simpleValue);
  }

  @Nonnull
  Alert<V> button(int buttonId, @Nonnull Supplier<V> valueGetter);

  @Nonnull
  default Alert<V> button(@Nonnull String text, @Nonnull V simpleValue) {
    return button(text, () -> simpleValue);
  }

  @Nonnull
  Alert<V> button(@Nonnull String text, @Nonnull Supplier<V> valueGetter);

  /**
   * Mark last added button as default (enter will hit it)
   */
  @Nonnull
  Alert<V> asDefaultButton();

  /**
   * Mark last added button as exit action (will be invoked if window closed by X)
   */
  @Nonnull
  Alert<V> asExitButton();

  @Nonnull
  default Alert<V> exitValue(@Nonnull V value) {
    return exitValue(() -> value);
  }

  @Nonnull
  Alert<V> exitValue(@Nonnull Supplier<V> valueGetter);

  /**
   * Default title is application name
   */
  @Nonnull
  Alert<V> title(@Nonnull String text);

  @Nonnull
  Alert<V> text(@Nonnull String text);

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  @Nonnull
  default AsyncResult<V> show(@Nullable Component component) {
    return show(TraverseUtil.getWindowAncestor(component));
  }

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  @Nonnull
  default AsyncResult<V> show() {
    return show(null);
  }

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  @Nonnull
  AsyncResult<V> show(@Nullable Window component);
}
