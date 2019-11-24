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
  public static ExtensionsArea getRootArea() {
    return Application.get().getExtensionsArea();
  }

  @Nonnull
  @Deprecated
  public static ExtensionsArea getArea(@Nullable ComponentManager componentManager) {
    return componentManager == null ? getRootArea() : componentManager.getExtensionsArea();
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
    ExtensionPoint<T> extensionPoint = componentManager.getExtensionsArea().getExtensionPoint(extensionPointName);
    return extensionPoint.getExtensions();
  }

  @Nonnull
  @Deprecated
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, @Nonnull Class<U> extClass) {
    for (T t : getExtensions(extensionPointName)) {
      if (extClass.isInstance(t)) {
        //noinspection unchecked
        return (U)t;
      }
    }
    throw new IllegalArgumentException("could not find extension implementation " + extClass);
  }

  @Nonnull
  @Deprecated
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, ComponentManager areaInstance, @Nonnull Class<U> extClass) {
    for (T t : getExtensions(extensionPointName, areaInstance)) {
      if (extClass.isInstance(t)) {
        //noinspection unchecked
        return (U)t;
      }
    }
    throw new IllegalArgumentException("could not find extension implementation " + extClass);
  }
}
