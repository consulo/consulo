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

import consulo.annotation.DeprecationInfo;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author AKireyev
 */
public interface ExtensionPoint<T> {
  enum Kind {
    INTERFACE,
    BEAN_CLASS
  }

  @Nonnull
  String getName();

  @Nonnull
  @SuppressWarnings("unchecked")
  @Deprecated
  @DeprecationInfo("Use #getExtensionList()")
  default T[] getExtensions() {
    List<T> list = getExtensionList();
    return list.toArray((T[])Array.newInstance(getExtensionClass(), list.size()));
  }

  default boolean hasAnyExtensions() {
    return !getExtensionList().isEmpty();
  }

  @Nonnull
  List<T> getExtensionList();

  @Nonnull
  Class<T> getExtensionClass();

  @Nonnull
  Kind getKind();

  @Nonnull
  String getClassName();

  @Nullable
  <K extends T> K findExtension(Class<K> extensionClass);

  void processWithPluginDescriptor(@Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer);
}
