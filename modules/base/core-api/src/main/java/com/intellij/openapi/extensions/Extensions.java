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
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Extensions {
  private Extensions() {
  }

  @Nonnull
  public static ExtensionsArea getRootArea() {
    return Application.get().getExtensionsArea();
  }

  @Nonnull
  public static ExtensionsArea getArea(@Nullable AreaInstance areaInstance) {
    return areaInstance == null ? getRootArea() : areaInstance.getExtensionsArea();
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
    return (T[])getExtensions(extensionPointName.getName(), Application.get());
  }

  @Nonnull
  @SuppressWarnings({"unchecked"})
  @Deprecated
  public static <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName, AreaInstance areaInstance) {
    return Extensions.<T>getExtensions(extensionPointName.getName(), areaInstance);
  }

  @Nonnull
  @Deprecated
  public static <T> T[] getExtensions(String extensionPointName, @Nullable AreaInstance areaInstance) {
    ExtensionsArea area = getArea(areaInstance);
    ExtensionPoint<T> extensionPoint = area.getExtensionPoint(extensionPointName);
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
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, AreaInstance areaInstance, @Nonnull Class<U> extClass) {
    for (T t : getExtensions(extensionPointName, areaInstance)) {
      if (extClass.isInstance(t)) {
        //noinspection unchecked
        return (U)t;
      }
    }
    throw new IllegalArgumentException("could not find extension implementation " + extClass);
  }
}
