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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import consulo.ui.util.TraverseUtil;
import consulo.util.concurrent.AsyncResult;

import org.jspecify.annotations.Nullable;
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
  static <L> Alert<L> create() {
    return UIInternal.get()._Alerts_create();
  }
  Alert<V> remember(AlertValueRemember<V> remember);
  Alert<V> asWarning();
  Alert<V> asError();
  Alert<V> asQuestion();
  default Alert<V> button(int buttonId, V simpleValue) {
    return button(buttonId, () -> simpleValue);
  }
  Alert<V> button(int buttonId, Supplier<V> valueGetter);
  default Alert<V> button(String text, V simpleValue) {
    return button(text, () -> simpleValue);
  }
  default Alert<V> button(String text, Supplier<V> valueGetter) {
    return button(LocalizeValue.of(text), valueGetter);
  }
  Alert<V> button(LocalizeValue text, Supplier<V> valueGetter);

  /**
   * Mark last added button as default (enter will hit it)
   */
  Alert<V> asDefaultButton();

  /**
   * Mark last added button as exit action (will be invoked if window closed by X)
   */
  Alert<V> asExitButton();
  default Alert<V> exitValue(V value) {
    return exitValue(() -> value);
  }
  Alert<V> exitValue(Supplier<V> valueGetter);
  @Deprecated
  @DeprecationInfo("Use with #title(LocalizeValue)")
  default Alert<V> title(String text) {
    return title(LocalizeValue.of(text));
  }

  /**
   * Default title is application name
   */
  Alert<V> title(LocalizeValue value);
  @Deprecated
  @DeprecationInfo("Use with #text(LocalizeValue)")
  default Alert<V> text(String text) {
    return text(LocalizeValue.of(text));
  }
  Alert<V> text(LocalizeValue value);

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  default AsyncResult<V> showAsync(@Nullable Component component) {
    return showAsync(TraverseUtil.getWindowAncestor(component));
  }

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  default AsyncResult<V> showAsync() {
    return showAsync((Window)null);
  }

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  AsyncResult<V> showAsync(@Nullable Window component);

  /**
   * Does not block UI thread
   */
  @RequiredUIAccess
  default AsyncResult<V> showAsync(WindowOwner component) {
    return showAsync(component.getWindow());
  }
}
