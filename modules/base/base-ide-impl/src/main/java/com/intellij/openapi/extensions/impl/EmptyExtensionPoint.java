/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.openapi.extensions.impl;

import com.intellij.openapi.extensions.ExtensionPoint;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 07/11/2021
 */
public final class EmptyExtensionPoint<T> implements ExtensionPoint<T> {
  private static final EmptyExtensionPoint ourInstance = new EmptyExtensionPoint();

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <K> K get() {
    return (K)ourInstance;
  }

  @Nonnull
  @Override
  public String getName() {
    return "";
  }

  @Nonnull
  @Override
  public List<T> getExtensionList() {
    return List.of();
  }

  @Nonnull
  @Override
  public Class<T> getExtensionClass() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return Kind.INTERFACE;
  }

  @Nonnull
  @Override
  public String getClassName() {
    return Object.class.getName();
  }

  @Override
  public void processWithPluginDescriptor(@Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {

  }

  @Nullable
  @Override
  public <K extends T> K findExtension(Class<K> extensionClass) {
    return null;
  }
}
