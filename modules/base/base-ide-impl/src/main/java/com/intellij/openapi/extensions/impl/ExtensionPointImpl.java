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

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.LoadingOrder;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.util.KeyedLazyInstanceEP;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.extensions.ExtensionExtender;
import consulo.logging.Logger;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author AKireyev
 */
public class ExtensionPointImpl<T> implements ExtensionPoint<T> {
  static class CacheValue<K> {
    final List<Pair<K, PluginDescriptor>> myExtensionCache;
    final List<K> myUnwrapExtensionCache;

    final List<ExtensionComponentAdapter<K>> myExtensionAdapters;

    CacheValue(@Nullable List<Pair<K, PluginDescriptor>> extensionCache, @Nullable List<ExtensionComponentAdapter<K>> extensionAdapters) {
      myExtensionCache = extensionCache;
      myUnwrapExtensionCache = extensionCache == null ? null : new UnwrapList<K>(extensionCache);
      myExtensionAdapters = extensionAdapters;
    }
  }

  static class UnwrapList<K> extends AbstractList<K> {
    private final List<Pair<K, PluginDescriptor>> myResult;

    UnwrapList(List<Pair<K, PluginDescriptor>> result) {
      myResult = result;
    }

    @Override
    public K get(int index) {
      return myResult.get(index).getFirst();
    }

    @Override
    public int size() {
      return myResult.size();
    }
  }

  private static final Logger LOG = Logger.getInstance(ExtensionPointImpl.class);

  private final ComponentManager myComponentManager;
  private final String myName;
  private final String myClassName;
  private final Kind myKind;
  @Nonnull
  private final PluginDescriptor myDescriptor;

  private Class<T> myExtensionClass;

  private volatile CacheValue<T> myCacheValue;

  private boolean myLocked;

  private final AtomicNotNullLazyValue<Map<Class, Object>> myInstanceOfCacheValue = AtomicNotNullLazyValue.createValue(ConcurrentHashMap::new);

  private final Runnable myCheckCanceled;

  public ExtensionPointImpl(@Nonnull String name,
                            @Nonnull String className,
                            @Nonnull Kind kind,
                            @Nonnull ComponentManager componentManager,
                            @Nonnull Runnable checkCanceled,
                            @Nonnull PluginDescriptor descriptor) {
    myName = name;
    myClassName = className;
    myKind = kind;
    myComponentManager = componentManager;
    myCheckCanceled = checkCanceled;
    myDescriptor = descriptor;

    myCacheValue = new CacheValue<>(null, new ArrayList<>());
  }

  public void setLocked() {
    myLocked = true;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
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

  @Nonnull
  @Override
  public List<T> getExtensionList() {
    CacheValue<T> cacheValue = myCacheValue;

    List<T> extensionCache = cacheValue.myUnwrapExtensionCache;
    if (extensionCache != null) {
      return extensionCache;
    }

    List<Pair<T, PluginDescriptor>> result = build(cacheValue.myExtensionAdapters, it -> myComponentManager.getInjectingContainer().getUnbindedInstance(it), true);
    CacheValue<T> value = setExtensionCache(result);
    return value.myUnwrapExtensionCache;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <K extends T> K findExtension(Class<K> extensionClass) {
    Map<Class, Object> value = myInstanceOfCacheValue.getValue();

    Object result = value.computeIfAbsent(extensionClass, aClass -> {
      K instance = ContainerUtil.findInstance(getExtensionList(), extensionClass);
      return instance == null ? ObjectUtil.NULL : instance;
    });

    return result == ObjectUtil.NULL ? null : (K)result;
  }

  @Override
  public void processWithPluginDescriptor(@Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    CacheValue<T> cacheValue = myCacheValue;

    List<Pair<T, PluginDescriptor>> extensionCache = cacheValue.myExtensionCache;
    if (extensionCache == null) {
      List<Pair<T, PluginDescriptor>> result = build(cacheValue.myExtensionAdapters, it -> myComponentManager.getInjectingContainer().getUnbindedInstance(it), true);
      CacheValue<T> value = setExtensionCache(result);
      extensionCache = value.myExtensionCache;
    }

    for (Pair<T, PluginDescriptor> pair : extensionCache) {
      consumer.accept(pair.getFirst(), pair.getSecond());
    }
  }

  @Override
  public boolean hasAnyExtensions() {
    CacheValue<T> cacheValue = myCacheValue;

    List<T> extensionCache = cacheValue.myUnwrapExtensionCache;
    // if we extensions already build - check list
    if (extensionCache != null) {
      return !extensionCache.isEmpty();
    }

    // if we have extenders we need build before
    if (!getExtenders().isEmpty()) {
      return !getExtensionList().isEmpty();
    }

    // at this moment myExtensionAdapters must exists
    return !cacheValue.myExtensionAdapters.isEmpty();
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private List<ExtensionExtender<T>> getExtenders() {
    // do not extend extender
    if (ExtensionExtender.EP_NAME.getName().equals(myName)) {
      return Collections.emptyList();
    }

    List<ExtensionExtender<T>> extenders = new ArrayList<>();

    for (KeyedLazyInstanceEP<ExtensionExtender> ep : ExtensionExtender.EP_NAME.getExtensionList()) {
      if (myName.equals(ep.getKey())) {
        try {
          ExtensionExtender extender = ep.getInstance();
          extenders.add(extender);
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }
    return extenders;
  }

  @Nonnull
  public CacheValue<T> setExtensionCache(@Nonnull List<Pair<T, PluginDescriptor>> list) {
    CacheValue<T> value = new CacheValue<>(list, null);
    myCacheValue = value;
    return value;
  }

  @Nonnull
  public List<Pair<T, PluginDescriptor>> buildUnsafe(Function<Class<T>, T> unbindedInstanceFun) {
    return build(myCacheValue.myExtensionAdapters, unbindedInstanceFun, false);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private List<Pair<T, PluginDescriptor>> build(List<ExtensionComponentAdapter<T>> extensionAdapters, Function<Class<T>, T> unbindedInstanceFunc, boolean withExtenders) {
    if (extensionAdapters == null) {
      throw new IllegalArgumentException("Data dropped...");
    }

    if (extensionAdapters.isEmpty()) {
      return Collections.emptyList();
    }

    myCheckCanceled.run();

    List<Pair<T, PluginDescriptor>> extensions = new ArrayList<>(extensionAdapters.size());
    List<ExtensionComponentAdapter<T>> adapters = new ArrayList<>(extensionAdapters);
    LoadingOrder.sort(adapters);

    Class<T> extensionClass = getExtensionClass();
    for (ExtensionComponentAdapter<T> adapter : adapters) {
      try {
        myCheckCanceled.run();

        T extension = adapter.getExtension(unbindedInstanceFunc);
        if (!extensionClass.isInstance(extension)) {
          LOG.error("Extension " + extension.getClass() + " does not implement " + extensionClass);
          continue;
        }

        Pair<T, PluginDescriptor> pair = Pair.create(extension, adapter.getPluginDescriptor());
        if (extensions.contains(pair)) {
          LOG.error("Extension " + extension.getClass() + " duplicated. EPName: " + getName());
          continue;
        }

        extensions.add(pair);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    if (withExtenders) {
      for (ExtensionExtender<T> extender : getExtenders()) {
        PluginDescriptor descriptor = PluginManager.getPlugin(extender.getClass());
        assert descriptor != null;
        extender.extend(myComponentManager, t -> extensions.add(Pair.create(t, descriptor)));
      }
    }

    Pair[] array = extensions.toArray(new Pair[extensions.size()]);
    return ContainerUtil.immutableList(array);
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
        @SuppressWarnings("unchecked") Class<T> extClass = (Class<T>)Class.forName(myClassName, true, pluginClassLoader);
        myExtensionClass = extensionClass = extClass;
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return extensionClass;
  }

  @Override
  public String toString() {
    return getName();
  }

  @SuppressWarnings("unchecked")
  public void registerExtensionAdapter(@Nonnull ExtensionComponentAdapter adapter) {
    if (myLocked) {
      throw new IllegalArgumentException("locked");
    }

    CacheValue<T> cacheValue = myCacheValue;

    if (cacheValue.myExtensionAdapters == null) {
      throw new IllegalArgumentException("point is immutable");
    }

    cacheValue.myExtensionAdapters.add(adapter);
  }
}
