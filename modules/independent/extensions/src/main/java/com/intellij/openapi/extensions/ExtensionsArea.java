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

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

/**
 * @author AKireyev
 */
public interface ExtensionsArea {
  boolean hasExtensionPoint(@NonNls @Nonnull String extensionPointName);

  @Nonnull
  <T> ExtensionPoint<T> getExtensionPoint(@NonNls @Nonnull String extensionPointName);

  @Nonnull
  <T> ExtensionPoint<T> getExtensionPoint(@Nonnull ExtensionPointName<T> extensionPointName);

  @Nonnull
  ExtensionPoint[] getExtensionPoints();


  String getId();

  // region Deprecated stuff
  @Nonnull
  @Deprecated
  default AreaPicoContainer getPicoContainer() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  default void registerExtensionPoint(@NonNls @Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass) {
    // nothing
  }

  @Deprecated
  default void registerExtensionPoint(@NonNls @Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull ExtensionPoint.Kind kind) {
    // nothing
  }

  @Deprecated
  default void registerExtensionPoint(@Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull PluginDescriptor descriptor) {
    // nothing
  }

  @Deprecated
  default void unregisterExtensionPoint(@NonNls @Nonnull String extensionPointName) {
  }

  @Deprecated
  default void registerExtensionPoint(@Nonnull String pluginName, @Nonnull Element extensionPointElement) {
    // nothing
  }

  @Deprecated
  default void registerExtensionPoint(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull Element extensionPointElement) {
    // nothing
  }

  @Deprecated
  default void registerExtension(@Nonnull String pluginName, @Nonnull Element extensionElement) {
    // nothing
  }

  @Deprecated
  default void registerExtension(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull Element extensionElement) {
    // nothing
  }
  // endregion
}
