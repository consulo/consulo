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

/**
 * @author AKireyev
 */
public interface ExtensionPoint<T> {
  @Nonnull
  String getName();
  AreaInstance getArea();

  /**
   * @deprecated use {@link #getClassName()} instead
   */
  @Nonnull
  String getBeanClassName();

  void registerExtension(@Nonnull T extension);
  void registerExtension(@Nonnull T extension, @Nonnull LoadingOrder order);

  @Nonnull
  T[] getExtensions();
  boolean hasAnyExtensions();

  @javax.annotation.Nullable
  T getExtension();
  boolean hasExtension(@Nonnull T extension);

  void unregisterExtension(@Nonnull T extension);

  void addExtensionPointListener(@Nonnull ExtensionPointListener<T> listener, @Nonnull Disposable parentDisposable);
  void addExtensionPointListener(@Nonnull ExtensionPointListener<T> listener);
  void removeExtensionPointListener(@Nonnull ExtensionPointListener<T> extensionPointListener);

  void reset();

  @Nonnull
  Class<T> getExtensionClass();

  @Nonnull
  Kind getKind();

  @Nonnull
  String getClassName();

  enum Kind {INTERFACE, BEAN_CLASS}
}
