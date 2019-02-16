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
package consulo.ui.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;
import consulo.ui.style.ColorKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 15-Oct-17
 */
@Deprecated
public interface SomeUIWrapper extends Component {
  @Nonnull
  UIDataObject dataObject();

  @Override
  default <T> void putUserData(@Nonnull Key<T> key, @javax.annotation.Nullable T value) {
    dataObject().putUserData(key, value);
  }

  @Nullable
  @Override
  default <T> T getUserData(@Nonnull Key<T> key) {
    return dataObject().getUserData(key);
  }

  @Override
  @Nonnull
  default Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
    return dataObject().addUserDataProvider(function);
  }

  @Nonnull
  @Override
  default <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @Nonnull T listener) {
    return dataObject().addListener(eventClass, listener);
  }

  @Nonnull
  @Override
  default <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass) {
    return dataObject().getDispatcher(eventClass);
  }

  @RequiredUIAccess
  @Override
  default void addBorder(@Nonnull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    dataObject().addBorder(borderPosition, borderStyle, colorKey, width);

    bordersChanged();
  }

  @RequiredUIAccess
  @Override
  default void removeBorder(@Nonnull BorderPosition borderPosition) {
    dataObject().removeBorder(borderPosition);

    bordersChanged();
  }

  default void bordersChanged() {
  }
}
