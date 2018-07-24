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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Extensions {
  @Nullable
  private static ExtensionsArea ourRootArea;

  public static void setRootArea(@Nullable AreaInstance area) {
    ourRootArea = area == null ? null : area.getExtensionsArea();
  }

  private Extensions() {
  }

  @Nonnull
  @Deprecated
  public static ExtensionsArea getRootArea() {
    if (ourRootArea == null) {
      throw new IllegalArgumentException("Application not initialized");
    }

    return ourRootArea;
  }

  @Nonnull
  public static ExtensionsArea getArea(@Nullable AreaInstance areaInstance) {
    if (ourRootArea == null) {
      throw new IllegalArgumentException("Application not initialized");
    }
    if (areaInstance == null) {
      return ourRootArea;
    }
    return areaInstance.getExtensionsArea();
  }

  @Nonnull
  public static <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensions(extensionPointName.getName(), null);
  }

  @Nonnull
  public static <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName, AreaInstance areaInstance) {
    return Extensions.<T>getExtensions(extensionPointName.getName(), areaInstance);
  }

  @Nonnull
  public static <T> T[] getExtensions(String extensionPointName, @Nullable AreaInstance areaInstance) {
    ExtensionsArea area = areaInstance == null ? getRootArea() : areaInstance.getExtensionsArea();
    ExtensionPoint<T> extensionPoint = area.getExtensionPoint(extensionPointName);
    return extensionPoint.getExtensions();
  }

  @Nonnull
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
  public static <T, U extends T> U findExtension(@Nonnull ExtensionPointName<T> extensionPointName, AreaInstance areaInstance, @Nonnull Class<U> extClass) {
    for (T t : getExtensions(extensionPointName, areaInstance)) {
      if (extClass.isInstance(t)) {
        //noinspection unchecked
        return (U)t;
      }
    }
    throw new IllegalArgumentException("could not find extension implementation " + extClass);
  }

  public static class SimpleLogProvider implements LogProvider {
    @Override
    public void error(String message) {
      new Throwable(message).printStackTrace();
    }

    @Override
    public void error(String message, @Nonnull Throwable t) {
      System.err.println(message);
      t.printStackTrace();
    }

    @Override
    public void error(@Nonnull Throwable t) {
      t.printStackTrace();
    }

    @Override
    public void warn(String message) {
      System.err.println(message);
    }

    @Override
    public void warn(String message, @Nonnull Throwable t) {
      System.err.println(message);
      t.printStackTrace();
    }

    @Override
    public void warn(@Nonnull Throwable t) {
      t.printStackTrace();
    }
  }
}
