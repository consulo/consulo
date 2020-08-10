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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ComponentManager;
import consulo.annotation.DeprecationInfo;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@DeprecationInfo("Use com.intellij.openapi.extensions.ExtensionPointName")
public class Extensions {
  private Extensions() {
  }
  @Nonnull
  @Deprecated
  public static Object[] getExtensions(@NonNls String extensionPointName) {
    return getExtensions(extensionPointName, null);
  }

  @Nonnull
  @SuppressWarnings({"unchecked"})
  @Deprecated
  public static <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName) {
    return extensionPointName.getExtensions();
  }

  @Nonnull
  @SuppressWarnings({"unchecked"})
  @Deprecated
  public static <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName, @Nullable ComponentManager areaInstance) {
    return areaInstance == null ? getExtensions(extensionPointName) : areaInstance.getExtensions(extensionPointName);
  }

  @Nonnull
  @Deprecated
  public static <T> T[] getExtensions(String extensionPointName, @Nullable ComponentManager target) {
    ComponentManager componentManager = target == null ? Application.get() : target;
    ExtensionPoint<T> extensionPoint = componentManager.getExtensionPoint(ExtensionPointName.create(extensionPointName));
    return extensionPoint.getExtensions();
  }

  @Nonnull
  @Deprecated
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, @Nonnull Class<U> extClass) {
    return extensionPointName.findExtensionOrFail(extClass);
  }

  @Nonnull
  @Deprecated
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, ComponentManager componentManager, @Nonnull Class<U> extClass) {
    return extensionPointName.findExtensionOrFail(componentManager, extClass);
  }
}
