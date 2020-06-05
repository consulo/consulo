/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.testFramework;

import consulo.disposer.Disposable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.LoadingOrder;

/**
 * @author VISTALL
 * @since 15:42/24.06.13
 */
@Deprecated
public class PsiTestExtensionUtil {

  public static <T> void registerExtension(final ExtensionPointName<T> name, final T t, final Disposable parentDisposable) {
    registerExtension(Extensions.getRootArea(), name, t, parentDisposable);
  }

  public static <T> void registerExtension(final ExtensionsArea area,
                                           final ExtensionPointName<T> name,
                                           final T t,
                                           final Disposable parentDisposable) {

  }

  public static <T> void registerExtension(final ExtensionsArea area,
                                           final ExtensionPointName<T> name,
                                           final T t,
                                           final Disposable parentDisposable,
                                           LoadingOrder loadingOrder) {

  }

  public static <T> void registerExtensionPointIfNeed(final ExtensionPointName<T> extensionPointName, final Class<T> aClass) {
    registerExtensionPointIfNeed(Extensions.getRootArea(), extensionPointName, aClass);
  }

  public static <T> void registerExtensionPointIfNeed(final ExtensionsArea area,
                                                      final ExtensionPointName<T> extensionPointName,
                                                      final Class<? extends T> aClass) {

  }
}
