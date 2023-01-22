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
package consulo.component.impl.internal.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionExtender;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.internal.inject.InjectingBindingHolder;
import consulo.component.internal.inject.InjectingContainer;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
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

    private LoadingOrder myLoadingOrder;

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
        myLoadingOrder = LoadingOrder.ANY;
      }
    }

    protected void reportFirstLastRestriction(@Nonnull PluginDescriptor apiPlugin) {
      // we allow it in platform impl
      if (PluginIds.isPlatformPlugin(myValue.getValue().getPluginId())) {
        return;
      }
      
      if (myLoadingOrder == LoadingOrder.FIRST || myLoadingOrder == LoadingOrder.LAST) {
        if (apiPlugin.getPluginId() != myValue.getValue().getPluginId()) {
          LOG.error("Usage order [first, last] is restricted for not owner plugin impl. Class: %s, Plugin: %s, Owner Plugin: %s"
                            .formatted(myValue.getKey().toString(), myValue.getValue().getPluginId().getIdString(), apiPlugin.getPluginId().getIdString()));
          myLoadingOrder = LoadingOrder.ANY;
        }
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

  private final String myApiClassName;
  private Class<T> myApiClass;

  private final ComponentManager myComponentManager;
  private final Runnable myCheckCanceled;
  private final ComponentScope myComponentScope;

  private Map<Class, Object> myInstanceOfCacheValue;
  private Map<ExtensionPointCacheKey, Object> myCaches;
  private long myModificationCount;
  private volatile CacheValue<T> myCacheValue;
  private volatile Boolean myHasAnyExtension;

  @SuppressWarnings("unchecked")
  public NewExtensionPointImpl(String apiClassName, List<InjectingBinding> bindings, ComponentManager componentManager, Runnable checkCanceled, ComponentScope componentScope) {
    myApiClassName = apiClassName;
    myCheckCanceled = checkCanceled;
    myComponentManager = componentManager;
    myComponentScope = componentScope;
    myCacheValue = new CacheValue<>(null, bindings);
  }

  public void initIfNeed(Class<T> apiClass) {
    if (myApiClass != null) {
      return;
    }

    myApiClass = apiClass;
  }

  public void reset(@Nonnull List<InjectingBinding> newBindings) {
    // reset instanceOf cache
    myInstanceOfCacheValue = null;
    // reset other caches
    myCaches = null;
    // reset all extension cache
    myCacheValue = new CacheValue<>(null, new ArrayList<>(newBindings));
    // reset api class
    myApiClass = null;
    // reset hasAnyExtensions
    myHasAnyExtension = null;
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

    Class<T> apiClass = getExtensionClass();

    ExtensionAPI annotation = apiClass.getAnnotation(ExtensionAPI.class);
    if (annotation == null) {
      throw new IllegalArgumentException(myApiClassName + " is not annotated by @ExtensionAPI");
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

        if (!apiClass.isInstance(extension)) {
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
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    if (apiClass != ExtensionExtender.class) {
      for (ExtensionExtender extender : Application.get().getExtensionPoint(ExtensionExtender.class).getExtensionList()) {
        if (extender.getExtensionClass() == apiClass) {
          PluginDescriptor descriptor = PluginManager.getPlugin(extender.getClass());
          assert descriptor != null;

          if (extender.hasAnyExtensions(myComponentManager)) {
            extender.extend(myComponentManager, it -> extensions.add(Pair.create((T)it, descriptor)));
          }
        }
      }
    }

    PluginDescriptor apiPlugin = PluginManager.getPlugin(apiClass);

    // prepare for sorting
    List<ExtensionImplOrderable<T>> orders = new ArrayList<>(extensions.size());
    for (Pair<T, PluginDescriptor> pair : extensions) {
      ExtensionImplOrderable<T> orderable = new ExtensionImplOrderable<>(pair);

      orderable.reportFirstLastRestriction(apiPlugin);
      
      orders.add(orderable);
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
    if (myHasAnyExtension != null) {
      return myHasAnyExtension;
    }

    CacheValue<T> cacheValue = myCacheValue;

    List<T> extensionCache = cacheValue.myUnwrapExtensionCache;
    // if we extensions already build - check list
    if (extensionCache != null) {
      return myHasAnyExtension = !extensionCache.isEmpty();
    }

    Class<T> extensionClass = getExtensionClass();

    // if we have extenders for this extension, always return
    if (extensionClass != ExtensionExtender.class) {
      for (ExtensionExtender extender : Application.get().getExtensionPoint(ExtensionExtender.class).getExtensionList()) {
        if (extender.getExtensionClass() == extensionClass && extender.hasAnyExtensions(myComponentManager)) {
          return myHasAnyExtension = true;
        }
      }
    }

    // at this moment myExtensionAdapters must exists
    return myHasAnyExtension = !cacheValue.myInjectingBindings.isEmpty();
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
  public String getClassName() {
    return myApiClassName;
  }

  @Nonnull
  @Override
  public Class<T> getExtensionClass() {
    return Objects.requireNonNull(myApiClass, "apiClass not initialized");
  }
}
