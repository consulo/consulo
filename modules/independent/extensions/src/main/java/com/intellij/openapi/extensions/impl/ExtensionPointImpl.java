/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.google.inject.Injector;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.*;
import com.intellij.util.containers.StringInterner;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

/**
 * @author AKireyev
 */
public class ExtensionPointImpl<T> implements ExtensionPoint<T> {
  private static final Logger LOG = Logger.getInstance(ExtensionPointImpl.class);

  private final AreaInstance myArea;
  private final String myName;
  private final String myClassName;
  private final Kind myKind;

  private final List<T> myExtensions = new ArrayList<T>();
  private volatile T[] myExtensionsCache;

  private final PluginDescriptor myDescriptor;

  private final Set<ExtensionComponentAdapter> myExtensionAdapters = new LinkedHashSet<>();

  private Class<T> myExtensionClass;

  private static final StringInterner INTERNER = new StringInterner();

  public ExtensionPointImpl(@Nonnull String name,
                            @Nonnull String className,
                            @Nonnull Kind kind,
                            @Nonnull ExtensionsAreaImpl owner,
                            AreaInstance area,
                            @Nonnull LogProvider logger,
                            @Nonnull PluginDescriptor descriptor) {
    synchronized (INTERNER) {
      myName = INTERNER.intern(name);
    }
    myClassName = className;
    myKind = kind;
    myArea = area;
    myDescriptor = descriptor;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public AreaInstance getArea() {
    return myArea;
  }

  @Nonnull
  @Override
  public String getBeanClassName() {
    return myClassName;
  }

  @Nonnull
  @Override
  public String getClassName() {
    return myClassName;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @Nonnull
  public PluginDescriptor getDescriptor() {
    return myDescriptor;
  }

  @Override
  @Nonnull
  public T[] getExtensions() {
    T[] result = myExtensionsCache;
    if (result == null) {
      synchronized (this) {
        result = myExtensionsCache;
        if (result == null) {
          processAdapters();

          Class<T> extensionClass = getExtensionClass();
          @SuppressWarnings("unchecked") T[] a = (T[])Array.newInstance(extensionClass, myExtensions.size());
          result = myExtensions.toArray(a);

          for (int i = result.length - 1; i >= 0; i--) {
            T extension = result[i];
            if (extension == null) {
              LOG.error(" null extension: " + myExtensions + ";\n" + " getExtensionClass(): " + extensionClass + ";\n");
            }
            if (i > 0 && extension == result[i - 1]) {
              LOG.error("Duplicate extension found: " +
                        extension +
                        "; " +
                        " Result:      " +
                        Arrays.toString(result) +
                        ";\n" +
                        " extensions: " +
                        myExtensions +
                        ";\n" +
                        " getExtensionClass(): " +
                        extensionClass +
                        ";\n" +
                        " size:" +
                        myExtensions.size() +
                        ";" +
                        result.length);
            }
          }

          myExtensionsCache = result;
        }
      }
    }
    return result;
  }

  @Override
  public boolean hasAnyExtensions() {
    final T[] cache = myExtensionsCache;
    if (cache != null) {
      return cache.length > 0;
    }
    synchronized (this) {
      return myExtensionAdapters.size() > 0;
    }
  }

  @SuppressWarnings("unchecked")
  private void processAdapters() {
    myExtensions.clear();
    Injector injector = myArea.getInjector();

    ArrayList<ExtensionComponentAdapter> sorted = new ArrayList<>(myExtensionAdapters);
    LoadingOrder.sort(sorted);
    for (Function<Injector, Object> adapter : sorted) {
      myExtensions.add((T)adapter.apply(injector));
    }
  }

  @Nonnull
  public Set<ExtensionComponentAdapter> getExtensionAdapters() {
    return myExtensionAdapters;
  }

  @Nonnull
  @Override
  public Class<T> getExtensionClass() {
    // racy single-check: we don't care whether the access to 'myExtensionClass' is thread-safe
    // but initial store in a local variable is crucial to prevent instruction reordering
    // see Item 71 in Effective Java or http://jeremymanson.blogspot.com/2008/12/benign-data-races-in-java.html
    Class<T> extensionClass = myExtensionClass;
    if (extensionClass == null) {
      try {
        ClassLoader pluginClassLoader = myDescriptor.getPluginClassLoader();
        @SuppressWarnings("unchecked") Class<T> extClass = pluginClassLoader == null ? (Class<T>)Class.forName(myClassName) : (Class<T>)Class.forName(myClassName, true, pluginClassLoader);
        myExtensionClass = extensionClass = extClass;
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return extensionClass;
  }

  public String toString() {
    return getName();
  }

  synchronized void registerExtensionAdapter(@Nonnull ExtensionComponentAdapter adapter) {
    myExtensionAdapters.add(adapter);
    clearCache();
  }

  private void clearCache() {
    myExtensionsCache = null;
  }
}
