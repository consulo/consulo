/*
 * Copyright 2013-2022 consulo.io
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
package consulo.component.impl.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionExtender;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.internal.InjectingBindingHolder;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.injecting.InjectingContainer;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 17-Jun-22
 */
public class NewExtensionPointImpl<T> implements ExtensionPoint<T> {
  private static final Logger LOG = Logger.getInstance(NewExtensionPointImpl.class);

  private static class ExtensionImplOrderable<K> implements LoadingOrder.Orderable {
    private final String myOrderId;
    private final Pair<K, PluginDescriptor> myValue;
    private final LoadingOrder myLoadingOrder;

    private ExtensionImplOrderable(Pair<K, PluginDescriptor> value) {
      myValue = value;

      K extensionImpl = myValue.getFirst();
      Class<?> extensionImplClass = extensionImpl.getClass();

      ExtensionImpl extensionImplAnnotation = extensionImplClass.getAnnotation(ExtensionImpl.class);
      // extension impl can be null if extension added by ExtensionExtender
      if (extensionImplAnnotation != null) {
        myOrderId = StringUtil.isEmptyOrSpaces(extensionImplAnnotation.id()) ? extensionImplClass.getSimpleName() : extensionImplAnnotation.id();
        myLoadingOrder = LoadingOrder.readOrder(extensionImplAnnotation.order());
      }
      else {
        myOrderId = extensionImplClass.getSimpleName();
        myLoadingOrder = LoadingOrder.LAST;
      }
    }

    @Nullable
    @Override
    public String getOrderId() {
      return myOrderId;
    }

    @Override
    public LoadingOrder getOrder() {
      return myLoadingOrder;
    }
  }

  private static class CacheValue<K> {
    final List<Pair<K, PluginDescriptor>> myExtensionCache;
    final List<K> myUnwrapExtensionCache;
    final List<InjectingBinding> myInjectingBindings;

    private CacheValue(@Nullable List<Pair<K, PluginDescriptor>> extensionCache, @Nullable List<InjectingBinding> injectingBindings) {
      myExtensionCache = extensionCache;
      myUnwrapExtensionCache = extensionCache == null ? null : new UnwrapList<K>(extensionCache);
      myInjectingBindings = injectingBindings;
    }
  }

  private static class UnwrapList<K> extends AbstractList<K> {
    private final List<Pair<K, PluginDescriptor>> myResult;

    private UnwrapList(List<Pair<K, PluginDescriptor>> result) {
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

  private Class<T> myApiClass;

  private final ComponentManager myComponentManager;
  private final Runnable myCheckCanceled;
  private final ComponentScope myComponentScope;

  private Map<Class, Object> myInstanceOfCacheValue;
  private Map<ExtensionPointCacheKey, Object> myCaches;
  private long myModificationCount;
  private volatile CacheValue<T> myCacheValue;

  @SuppressWarnings("unchecked")
  public NewExtensionPointImpl(Class apiClass, List<InjectingBinding> bindings, ComponentManager componentManager, Runnable checkCanceled, ComponentScope componentScope) {
    myApiClass = apiClass;
    myCheckCanceled = checkCanceled;
    myComponentManager = componentManager;
    myComponentScope = componentScope;
    myCacheValue = new CacheValue<>(null, bindings);
  }

  public void reset(@Nonnull List<InjectingBinding> newBindings) {
    // reset instanceOf cache
    myInstanceOfCacheValue = null;
    // reset other caches
    myCaches = null;
    // reset all extension cache
    myCacheValue = new CacheValue<>(null, new ArrayList<>(newBindings));
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <K extends T> K findExtension(Class<K> extensionClass) {
    Map<Class, Object> instanceOfCacheValue = myInstanceOfCacheValue;
    if (instanceOfCacheValue == null) {
      instanceOfCacheValue = myInstanceOfCacheValue = Maps.newConcurrentHashMap(HashingStrategy.identity());
    }

    Object result = instanceOfCacheValue.computeIfAbsent(extensionClass, aClass -> {
      K instance = ContainerUtil.findInstance(getExtensionList(), extensionClass);
      return instance == null ? ObjectUtil.NULL : instance;
    });

    return result == ObjectUtil.NULL ? null : (K)result;
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <K> K getOrBuildCache(@Nonnull ExtensionPointCacheKey<T, K> key) {
    Map<ExtensionPointCacheKey, Object> caches = myCaches;
    if (caches == null) {
      caches = myCaches = Maps.newConcurrentHashMap(HashingStrategy.identity());
    }

    return (K)caches.computeIfAbsent(key, k -> key.getFactory().apply(getExtensionList()));
  }

  @SuppressWarnings("unchecked")
  private List<Pair<T, PluginDescriptor>> build(@Nullable List<InjectingBinding> injectingBindings) {
    if (injectingBindings == null) {
      throw new IllegalArgumentException("cache dropped");
    }

    ExtensionAPI annotation = myApiClass.getAnnotation(ExtensionAPI.class);
    if (annotation == null) {
      throw new IllegalArgumentException(myApiClass + " is not annotated by @ExtensionAPI");
    }

    ComponentScope value = annotation.value();
    if (value != myComponentScope) {
      throw new IllegalArgumentException("Wrong extension scope " + value + " vs " + myApiClass);
    }

    InjectingContainer injectingContainer = myComponentManager.getInjectingContainer();

    List<Pair<T, PluginDescriptor>> extensions = new ArrayList<>(injectingBindings.size());
    for (InjectingBinding binding : injectingBindings) {
      if (!InjectingBindingHolder.isValid(binding, myComponentManager.getProfiles())) {
        continue;
      }

      T extension;
      try {
        myCheckCanceled.run();

        extension = (T)injectingContainer.getUnbindedInstance(binding.getImplClass(), binding.getParameterTypes(), binding::create);

        if (!myApiClass.isInstance(extension)) {
          LOG.error("Extension " + extension.getClass() + " does not implement " + myApiClass);
          continue;
        }

        PluginDescriptor plugin = PluginManager.getPlugin(binding.getImplClass());
        Pair<T, PluginDescriptor> pair = Pair.create(extension, plugin);
        if (extensions.contains(pair)) {
          LOG.error("Extension " + extension.getClass() + " duplicated. Extension: " + getName());
          continue;
        }


        extensions.add(pair);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    if (myApiClass != ExtensionExtender.class) {
      for (ExtensionExtender extender : Application.get().getExtensionPoint(ExtensionExtender.class).getExtensionList()) {
        if (extender.getExtensionClass() == myApiClass) {
          PluginDescriptor descriptor = PluginManager.getPlugin(extender.getClass());
          assert descriptor != null;

          if (extender.hasAnyExtensions()) {
            extender.extend(myComponentManager, it -> extensions.add(Pair.create((T)it, descriptor)));
          }
        }
      }
    }

    // prepare for sorting
    List<ExtensionImplOrderable<T>> orders = new ArrayList<>(extensions.size());
    for (Pair<T, PluginDescriptor> pair : extensions) {
      orders.add(new ExtensionImplOrderable<>(pair));
    }

    LoadingOrder.sort(orders);

    // set new order
    for (int i = 0; i < orders.size(); i++) {
      extensions.set(i, orders.get(i).myValue);
    }

    Pair[] array = extensions.toArray(new Pair[extensions.size()]);
    return List.of(array);
  }


  @Override
  @SuppressWarnings("unchecked")
  public void processWithPluginDescriptor(@Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {
    CacheValue<T> cacheValue = myCacheValue;

    List<Pair<T, PluginDescriptor>> extensionCache = cacheValue.myExtensionCache;
    if (extensionCache == null) {
      List<Pair<T, PluginDescriptor>> result = build(cacheValue.myInjectingBindings);
      CacheValue<T> value = set(result);
      extensionCache = value.myExtensionCache;
    }

    for (Pair<T, PluginDescriptor> pair : extensionCache) {
      consumer.accept(pair.getFirst(), pair.getSecond());
    }
  }

  public CacheValue<T> set(@Nonnull List<Pair<T, PluginDescriptor>> list) {
    CacheValue<T> value = new CacheValue<>(list, null);
    myCacheValue = value;

    myModificationCount++;
    return value;
  }

  @Override
  public boolean hasAnyExtensions() {
    CacheValue<T> cacheValue = myCacheValue;

    List<T> extensionCache = cacheValue.myUnwrapExtensionCache;
    // if we extensions already build - check list
    if (extensionCache != null) {
      return !extensionCache.isEmpty();
    }

    // if we have extenders for this extension, always return
    if (myApiClass != ExtensionExtender.class) {
      for (ExtensionExtender extender : Application.get().getExtensionPoint(ExtensionExtender.class).getExtensionList()) {
        if (extender.getExtensionClass() == myApiClass && extender.hasAnyExtensions()) {
          return true;
        }
      }
    }

    // at this moment myExtensionAdapters must exists
    return !cacheValue.myInjectingBindings.isEmpty();
  }

  @Override
  public long getModificationCount() {
    return myModificationCount;
  }

  @Nonnull
  @Override
  public List<T> getExtensionList() {
    CacheValue<T> cacheValue = myCacheValue;

    List<T> extensionCache = cacheValue.myUnwrapExtensionCache;
    if (extensionCache != null) {
      return extensionCache;
    }

    List<Pair<T, PluginDescriptor>> result = build(cacheValue.myInjectingBindings);
    CacheValue<T> value = set(result);
    return value.myUnwrapExtensionCache;
  }

  @Nonnull
  @Override
  public Class<T> getExtensionClass() {
    return myApiClass;
  }
}
