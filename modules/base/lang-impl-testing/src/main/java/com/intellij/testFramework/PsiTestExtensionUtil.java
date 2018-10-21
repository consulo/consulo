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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.*;
import com.intellij.openapi.util.Disposer;

import java.lang.reflect.Modifier;

/**
 * @author VISTALL
 * @since 15:42/24.06.13
 */
public class PsiTestExtensionUtil {

  public static <T> void registerExtension(final ExtensionPointName<T> name, final T t, final Disposable parentDisposable) {
    registerExtension(Extensions.getRootArea(), name, t, parentDisposable);
  }

  public static <T> void registerExtension(final ExtensionsArea area,
                                           final ExtensionPointName<T> name,
                                           final T t,
                                           final Disposable parentDisposable) {
    final ExtensionPoint<T> extensionPoint = area.getExtensionPoint(name.getName());
    registerExtensionPointIfNeed(area, name, (Class<T>)t.getClass());
    extensionPoint.registerExtension(t);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        extensionPoint.unregisterExtension(t);
      }
    });
  }

  public static <T> void registerExtension(final ExtensionsArea area,
                                           final ExtensionPointName<T> name,
                                           final T t,
                                           final Disposable parentDisposable,
                                           LoadingOrder loadingOrder) {
    final ExtensionPoint<T> extensionPoint = area.getExtensionPoint(name.getName());
    registerExtensionPointIfNeed(area, name, (Class<T>)t.getClass());
    extensionPoint.registerExtension(t, loadingOrder);

    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        extensionPoint.unregisterExtension(t);
      }
    });
  }

  public static <T> void registerExtensionPointIfNeed(final ExtensionPointName<T> extensionPointName, final Class<T> aClass) {
    registerExtensionPointIfNeed(Extensions.getRootArea(), extensionPointName, aClass);
  }

  public static <T> void registerExtensionPointIfNeed(final ExtensionsArea area,
                                                      final ExtensionPointName<T> extensionPointName,
                                                      final Class<? extends T> aClass) {
    final String name = extensionPointName.getName();
    if (!area.hasExtensionPoint(name)) {
      ExtensionPoint.Kind kind = aClass.isInterface() || (aClass.getModifiers() & Modifier.ABSTRACT) != 0
                                 ? ExtensionPoint.Kind.INTERFACE
                                 : ExtensionPoint.Kind.BEAN_CLASS;
      //area.registerExtensionPoint(name, aClass.getName(), kind);
    }
  }
}
