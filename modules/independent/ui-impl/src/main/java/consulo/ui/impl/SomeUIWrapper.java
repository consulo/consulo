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

import com.intellij.openapi.util.Key;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.style.ColorKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 15-Oct-17
 */
public interface SomeUIWrapper extends Component {
  @NotNull
  UIDataObject dataObject();

  @Override
  default <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    dataObject().putUserData(key, value);
  }

  @Nullable
  @Override
  default <T> T getUserData(@NotNull Key<T> key) {
    return dataObject().getUserData(key);
  }

  @NotNull
  @Override
  default <T> Runnable addUserDataProvider(@NotNull Key<T> key, @NotNull Supplier<T> supplier) {
    return addUserDataProvider(k -> k == key ? supplier.get() : null);
  }

  @Override
  @NotNull
  default Runnable addUserDataProvider(@NotNull Function<Key<?>, Object> function) {
    return dataObject().addUserDataProvider(function);
  }

  @NotNull
  @Override
  default <T extends EventListener> Runnable addListener(@NotNull Class<T> eventClass, @NotNull T listener) {
    return dataObject().addListener(eventClass, listener);
  }

  @NotNull
  @Override
  default <T extends EventListener> T getListenerDispatcher(@NotNull Class<T> eventClass) {
    return dataObject().getDispatcher(eventClass);
  }

  @RequiredUIAccess
  @Override
  default void addBorder(@NotNull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    dataObject().addBorder(borderPosition, borderStyle, colorKey, width);
  }

  @RequiredUIAccess
  @Override
  default void removeBorder(@NotNull BorderPosition borderPosition) {
    dataObject().removeBorder(borderPosition);
  }
}
