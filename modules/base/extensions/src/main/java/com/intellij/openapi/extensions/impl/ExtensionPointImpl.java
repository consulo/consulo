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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.*;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.StringInterner;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

/**
 * @author AKireyev
 */
public class ExtensionPointImpl<T> implements ExtensionPoint<T> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.extensions.impl.ExtensionPointImpl");

  private final AreaInstance myArea;
  private final String myName;
  private final String myClassName;
  private final Kind myKind;

  private final List<T> myExtensions = new ArrayList<>();
  private volatile T[] myExtensionsCache;

  private final PluginDescriptor myDescriptor;

  private final Set<ExtensionComponentAdapter> myExtensionAdapters = new LinkedHashSet<>();
  private final List<ExtensionPointListener<T>> myEPListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<ExtensionComponentAdapter<T>> myLoadedAdapters = new ArrayList<>();

  private Class<T> myExtensionClass;

  private static final StringInterner INTERNER = new StringInterner();

  public ExtensionPointImpl(@Nonnull String name, @Nonnull String className, @Nonnull Kind kind, AreaInstance area, @Nonnull PluginDescriptor descriptor) {
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

  @Override
  public void registerExtension(@Nonnull T extension) {
    registerExtension(extension, LoadingOrder.ANY);
  }

  @Nonnull
  public PluginDescriptor getDescriptor() {
    return myDescriptor;
  }

  @Override
  public synchronized void registerExtension(@Nonnull T extension, @Nonnull LoadingOrder order) {
    assert myExtensions.size() == myLoadedAdapters.size();

    ExtensionComponentAdapter<T> adapter = new ObjectComponentAdapter<>(extension, order);

    if (LoadingOrder.ANY == order) {
      int index = myLoadedAdapters.size();
      if (index > 0) {
        ExtensionComponentAdapter lastAdapter = myLoadedAdapters.get(index - 1);
        if (lastAdapter.getOrder() == LoadingOrder.LAST) {
          index--;
        }
      }
      registerExtension(extension, adapter, index, true);
    }
    else {
      registerExtensionAdapter(adapter);
      processAdapters(aClass -> myArea.getInjectingContainer().getUnbindedInstance(aClass));
    }
  }

  private void registerExtension(@Nonnull T extension, @Nonnull ExtensionComponentAdapter adapter, int index, boolean runNotifications) {
    if (myExtensions.contains(extension)) {
      LOG.error("Extension was already added: " + extension);
      return;
    }

    Class<T> extensionClass = getExtensionClass();
    if (!extensionClass.isInstance(extension)) {
      LOG.error("Extension " + extension.getClass() + " does not implement " + extensionClass);
      return;
    }

    myExtensions.add(index, extension);
    myLoadedAdapters.add(index, adapter);

    if (runNotifications) {
      clearCache();

      if (!adapter.isNotificationSent()) {
        if (extension instanceof Extension) {
          try {
            ((Extension)extension).extensionAdded(this);
          }
          catch (Throwable e) {
            LOG.error(e);
          }
        }

        notifyListenersOnAdd(extension, adapter.getPluginDescriptor());
        adapter.setNotificationSent(true);
      }
    }
  }

  private void notifyListenersOnAdd(@Nonnull T extension, final PluginDescriptor pluginDescriptor) {
    for (ExtensionPointListener<T> listener : myEPListeners) {
      try {
        listener.extensionAdded(extension, pluginDescriptor);
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  @Override
  @Nonnull
  public T[] getExtensions() {
    return getExtensions(aClass -> myArea.getInjectingContainer().getUnbindedInstance(aClass));
  }

  public T[] getExtensions(Function<Class<T>, T> unbindedInstanceFunc) {
    T[] result = myExtensionsCache;
    if (result == null) {
      synchronized (this) {
        result = myExtensionsCache;
        if (result == null) {
          processAdapters(unbindedInstanceFunc);

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
      return myExtensionAdapters.size() + myLoadedAdapters.size() > 0;
    }
  }

  private void processAdapters(Function<Class<T>, T> unbindedInstanceFunc) {
    int totalSize = myExtensionAdapters.size() + myLoadedAdapters.size();
    if (totalSize != 0) {
      List<ExtensionComponentAdapter> adapters = ContainerUtil.newArrayListWithCapacity(totalSize);
      adapters.addAll(myExtensionAdapters);
      adapters.addAll(myLoadedAdapters);
      LoadingOrder.sort(adapters);
      myExtensionAdapters.clear();
      myExtensionAdapters.addAll(adapters);

      Set<ExtensionComponentAdapter> loaded = ContainerUtil.newHashOrEmptySet(myLoadedAdapters);
      myExtensions.clear();
      myLoadedAdapters.clear();

      for (ExtensionComponentAdapter adapter : adapters) {
        try {
          @SuppressWarnings("unchecked") T extension = (T)adapter.getExtension(unbindedInstanceFunc);
          registerExtension(extension, adapter, myExtensions.size(), !loaded.contains(adapter));
          myExtensionAdapters.remove(adapter);
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception e) {
          LOG.error(e);
          myExtensionAdapters.remove(adapter);
        }
      }
    }
  }

  @Override
  @Nullable
  public T getExtension() {
    T[] extensions = getExtensions();
    return extensions.length == 0 ? null : extensions[0];
  }

  @Override
  public synchronized boolean hasExtension(@Nonnull T extension) {
    processAdapters(aClass -> myArea.getInjectingContainer().getUnbindedInstance(aClass));
    return myExtensions.contains(extension);
  }

  @Override
  public synchronized void unregisterExtension(@Nonnull final T extension) {
    processAdapters(aClass -> myArea.getInjectingContainer().getUnbindedInstance(aClass));
    unregisterExtension(extension, null);
  }

  private int getExtensionIndex(@Nonnull T extension) {
    int i = myExtensions.indexOf(extension);
    if (i == -1) {
      throw new IllegalArgumentException("Extension to be removed not found: " + extension);
    }
    return i;
  }

  private void unregisterExtension(@Nonnull T extension, PluginDescriptor pluginDescriptor) {
    int index = getExtensionIndex(extension);

    myExtensions.remove(index);
    myLoadedAdapters.remove(index);
    clearCache();

    notifyListenersOnRemove(extension, pluginDescriptor);

    if (extension instanceof Extension) {
      try {
        ((Extension)extension).extensionRemoved(this);
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  private void notifyListenersOnRemove(@Nonnull T extensionObject, PluginDescriptor pluginDescriptor) {
    for (ExtensionPointListener<T> listener : myEPListeners) {
      try {
        listener.extensionRemoved(extensionObject, pluginDescriptor);
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void addExtensionPointListener(@Nonnull final ExtensionPointListener<T> listener, @Nonnull Disposable parentDisposable) {
    addExtensionPointListener(listener, true, parentDisposable);
  }

  public synchronized void addExtensionPointListener(@Nonnull final ExtensionPointListener<T> listener, final boolean invokeForLoadedExtensions, @Nonnull Disposable parentDisposable) {
    if (invokeForLoadedExtensions) {
      addExtensionPointListener(listener);
    }
    else {
      myEPListeners.add(listener);
    }
    Disposer.register(parentDisposable, () -> removeExtensionPointListener(listener, invokeForLoadedExtensions));
  }

  @Override
  public synchronized void addExtensionPointListener(@Nonnull ExtensionPointListener<T> listener) {
    processAdapters(aClass -> myArea.getInjectingContainer().getUnbindedInstance(aClass));
    if (myEPListeners.add(listener)) {
      for (ExtensionComponentAdapter<T> componentAdapter : new ArrayList<>(myLoadedAdapters)) {
        try {
          T extension = componentAdapter.getExtension(aClass -> myArea.getInjectingContainer().getUnbindedInstance(aClass));
          listener.extensionAdded(extension, componentAdapter.getPluginDescriptor());
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }
  }

  @Override
  public void removeExtensionPointListener(@Nonnull ExtensionPointListener<T> listener) {
    removeExtensionPointListener(listener, true);
  }

  private synchronized void removeExtensionPointListener(@Nonnull ExtensionPointListener<T> listener, boolean invokeForLoadedExtensions) {
    if (myEPListeners.remove(listener) && invokeForLoadedExtensions) {
      for (ExtensionComponentAdapter<T> componentAdapter : new ArrayList<>(myLoadedAdapters)) {
        try {
          T extension = componentAdapter.getExtension(aClass -> myArea.getInjectingContainer().getUnbindedInstance(aClass));
          listener.extensionRemoved(extension, componentAdapter.getPluginDescriptor());
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }
  }

  @Override
  public synchronized void reset() {
    myExtensionAdapters.clear();
    for (T extension : getExtensions()) {
      unregisterExtension(extension);
    }
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

  private static class ObjectComponentAdapter<K> extends ExtensionComponentAdapter<K> {
    private final K myExtension;
    private final LoadingOrder myLoadingOrder;

    private ObjectComponentAdapter(@Nonnull K extension, @Nonnull LoadingOrder loadingOrder) {
      super(extension.getClass().getName(), null, null, false);
      myExtension = extension;
      myLoadingOrder = loadingOrder;
    }

    @Override
    public K getExtension(Function<Class<K>, K> getUnbindedInstanceFunc) {
      return myExtension;
    }

    @Override
    public LoadingOrder getOrder() {
      return myLoadingOrder;
    }

    @Override
    @Nullable
    public String getOrderId() {
      return null;
    }

    @Override
    @NonNls
    public Element getDescribingElement() {
      return new Element("RuntimeExtension: " + myExtension);
    }
  }
}
