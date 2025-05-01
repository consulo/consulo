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
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionExtender;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionWalker;
import consulo.component.extension.preview.ExtensionPreviewRecorder;
import consulo.component.internal.ExtensionInstanceRef;
import consulo.component.internal.ExtensionLogger;
import consulo.component.internal.inject.InjectingBindingHolder;
import consulo.component.internal.inject.InjectingContainer;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2022-06-17
 */
public class NewExtensionPointImpl<T> implements ExtensionPoint<T> {
    private static final Logger LOG = Logger.getInstance(NewExtensionPointImpl.class);

    private static class CacheValue<K> {
        final List<Pair<K, PluginDescriptor>> myExtensionCache;
        final List<K> myUnwrapExtensionCache;
        final List<InjectingBinding> myInjectingBindings;

        private CacheValue(@Nullable List<Pair<K, PluginDescriptor>> extensionCache, @Nullable List<InjectingBinding> injectingBindings) {
            myExtensionCache = extensionCache;
            myUnwrapExtensionCache = extensionCache == null ? null : new UnwrapList<>(extensionCache);
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
    public NewExtensionPointImpl(
        String apiClassName,
        List<InjectingBinding> bindings,
        ComponentManager componentManager,
        Runnable checkCanceled,
        ComponentScope componentScope
    ) {
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

    @Nonnull
    @Override
    public List<T> sort(List<T> extensionsList) {
        return sortImpl(extensionsList);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static <K> List<K> sortImpl(List<K> extensionsList) {
        List<LoadingOrder.Orderable> orders = new ArrayList<>(extensionsList.size());
        for (K extension : extensionsList) {
            PluginDescriptor plugin = PluginManager.getPlugin(extension.getClass());
            if (plugin == null) {
                orders.add(new InvalidOrderable<>(extension));
            }
            else {
                orders.add(new ExtensionImplOrderable<>(Pair.create(extension, plugin)));
            }
        }
        LoadingOrder.sort(orders);
        return orders.stream().map(orderable -> (K)orderable.getObjectValue()).toList();
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

        return (K)caches.computeIfAbsent(key, k -> key.getFactory().apply(new ExtensionWalker<>() {
            @Override
            public void walk(@Nonnull Consumer<T> consumer) {
                forEachExtensionSafe(consumer);
            }

            @Nonnull
            @Override
            public Function<List<T>, List<T>> sorter() {
                return NewExtensionPointImpl::sortImpl;
            }
        }));
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

                boolean isRootManager = myComponentManager.getParent() == null;

                ExtensionInstanceRef instanceRef = null;

                if (isRootManager) {
                    instanceRef = new ExtensionInstanceRef();
                    ExtensionInstanceRef.CURRENT_CREATION.set(instanceRef);
                }

                extension = (T)injectingContainer.getUnbindedInstance(binding.getImplClass(), binding.getParameterTypes(), binding::create);

                if (instanceRef != null) {
                    ExtensionInstanceRef.CURRENT_CREATION.remove();

                    if (instanceRef.setter != null) {
                        instanceRef.setter.accept(extension);
                    }
                }

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

        // do not allow extend extenders, and preview recorder
        if (apiClass != ExtensionExtender.class && apiClass != ExtensionPreviewRecorder.class) {
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
            extensions.set(i, orders.get(i).getValue());
        }

        Pair[] array = extensions.toArray(new Pair[extensions.size()]);
        return List.of(array);
    }

    @Nonnull
    private List<Pair<T, PluginDescriptor>> buildOrGet() {
        CacheValue<T> cacheValue = myCacheValue;

        List<Pair<T, PluginDescriptor>> extensionCache = cacheValue.myExtensionCache;
        if (extensionCache == null) {
            List<Pair<T, PluginDescriptor>> result = build(cacheValue.myInjectingBindings);
            CacheValue<T> value = set(result);
            extensionCache = value.myExtensionCache;
        }
        return extensionCache;
    }

    @Nullable
    @Override
    public T findFirstSafe(@Nonnull Predicate<T> predicate) {
        List<Pair<T, PluginDescriptor>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).getKey();
            try {
                if (predicate.test(t)) {
                    return t;
                }
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public <R> R computeSafeIfAny(@Nonnull Function<? super T, ? extends R> processor) {
        List<Pair<T, PluginDescriptor>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).getKey();
            try {
                R r = processor.apply(t);
                if (r != null) {
                    return r;
                }
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public <R, CR extends Collection<? super R>> CR collectMapped(@Nonnull CR results, @Nonnull Function<? super T, ? extends R> processor) {
        List<Pair<T, PluginDescriptor>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).getKey();
            try {
                R result = processor.apply(t);
                if (result != null) {
                    results.add(result);
                }
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
        return results;
    }

    @Nonnull
    @Override
    public <CE extends Collection<T>> CE collectFiltered(@Nonnull CE results, @Nonnull Predicate<? super T> predicate) {
        List<Pair<T, PluginDescriptor>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).getKey();
            try {
                if (predicate.test(t)) {
                    results.add(t);
                }
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
        return results;
    }

    @Override
    public void forEachExtensionSafe(@Nonnull Consumer<? super T> consumer) {
        List<Pair<T, PluginDescriptor>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).getKey();
            try {
                consumer.accept(t);
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
    }

    @Override
    public void forEachBreakable(@Nonnull Function<? super T, Flow> breakableConsumer) {
        List<Pair<T, PluginDescriptor>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).getKey();
            try {
                if (breakableConsumer.apply(t) == Flow.BREAK) {
                    break;
                }
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processWithPluginDescriptor(@Nonnull BiConsumer<? super T, ? super PluginDescriptor> consumer) {
        List<Pair<T, PluginDescriptor>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < extensionCache.size(); i++) {
            Pair<T, PluginDescriptor> pair = extensionCache.get(i);
            try {
                consumer.accept(pair.getKey(), pair.getSecond());
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, pair.getKey());
            }
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
