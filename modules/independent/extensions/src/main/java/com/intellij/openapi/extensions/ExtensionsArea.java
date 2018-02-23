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
import org.picocontainer.PicoContainer;

/**
 * @author AKireyev
 */
public interface ExtensionsArea  {
  void registerExtensionPoint(@NonNls @Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass);
  void registerExtensionPoint(@NonNls @Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull ExtensionPoint.Kind kind);
  void registerExtensionPoint(@Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull PluginDescriptor descriptor);
  void unregisterExtensionPoint(@NonNls @Nonnull String extensionPointName);

  boolean hasExtensionPoint(@NonNls @Nonnull String extensionPointName);
  @Nonnull
  <T> ExtensionPoint<T> getExtensionPoint(@NonNls @Nonnull String extensionPointName);

  @Nonnull
  <T> ExtensionPoint<T> getExtensionPoint(@Nonnull ExtensionPointName<T> extensionPointName);

  @Nonnull
  ExtensionPoint[] getExtensionPoints();
  void suspendInteractions();
  void resumeInteractions();

  void killPendingInteractions();

  void addAvailabilityListener(@Nonnull String extensionPointName, @Nonnull ExtensionPointAvailabilityListener listener);

  @Nonnull
  AreaPicoContainer getPicoContainer();
  void registerExtensionPoint(@Nonnull String pluginName, @Nonnull Element extensionPointElement);
  void registerExtensionPoint(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull Element extensionPointElement);
  void registerExtension(@Nonnull String pluginName, @Nonnull Element extensionElement);

  void registerExtension(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull Element extensionElement);

  @Nonnull
  PicoContainer getPluginContainer(@Nonnull String pluginName);

  String getAreaClass();
}
