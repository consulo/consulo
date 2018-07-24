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
package com.intellij.openapi.extensions;

import com.intellij.openapi.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author AKireyev
 */
public interface ExtensionPoint<T> {
  @Nonnull
  String getName();

  @Nonnull
  AreaInstance getArea();

  /**
   * @deprecated use {@link #getClassName()} instead
   */
  @Nonnull
  String getBeanClassName();

  @Deprecated
  default void registerExtension(@Nonnull T extension) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  default void registerExtension(@Nonnull T extension, @Nonnull LoadingOrder order) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  T[] getExtensions();

  default boolean hasAnyExtensions() {
    return getExtensions().length > 0;
  }

  @Nullable
  default T getExtension() {
    T[] extensions = getExtensions();
    return extensions.length == 0 ? null : extensions[0];
  }

  default boolean hasExtension(@Nonnull T extension) {
    for (T t : getExtensions()) {
      if (t == extension) {
        return true;
      }
    }
    return false;
  }

  default void unregisterExtension(@Nonnull T extension) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  default void addExtensionPointListener(@Nonnull ExtensionPointListener<T> listener, @Nonnull Disposable parentDisposable) {
  }

  @Deprecated
  default void addExtensionPointListener(@Nonnull ExtensionPointListener<T> listener) {
  }

  @Deprecated
  default void removeExtensionPointListener(@Nonnull ExtensionPointListener<T> extensionPointListener) {
  }

  @Deprecated
  default void reset() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  Class<T> getExtensionClass();

  @Nonnull
  Kind getKind();

  @Nonnull
  String getClassName();

  enum Kind {
    INTERFACE,
    BEAN_CLASS
  }
}
