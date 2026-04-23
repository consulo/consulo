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
package consulo.component.internal;

import consulo.annotation.InheritCallerContext;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionExtender;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionWalker;
import consulo.component.extension.preview.ExtensionPreviewRecorder;
import consulo.component.internal.inject.InjectingBindingHolderImpl;
import consulo.component.internal.inject.InjectingContainer;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * @author VISTALL
 * @since 2022-06-17
 */
public class NewExtensionPointImpl<T> implements ExtensionPoint<T> {
    private static final Logger LOG = Logger.getInstance(NewExtensionPointImpl.class);

    private static class CacheValue<K> {
        final @Nullable List<ExtensionValue<K>> myExtensionCache;
        final @Nullable List<K> myUnwrapExtensionCache;
        final @Nullable List<InjectingBinding> myInjectingBindings;

        private CacheValue(@Nullable List<ExtensionValue<K>> extensionCache, @Nullable List<InjectingBinding> injectingBindings) {
            myExtensionCache = extensionCache;
            myUnwrapExtensionCache = extensionCache == null ? null : new UnwrapList<>(extensionCache);
            myInjectingBindings = injectingBindings;
        }
    }

    private static class UnwrapList<K> extends AbstractList<K> {
        private final List<ExtensionValue<K>> myResult;

        private UnwrapList(List<ExtensionValue<K>> result) {
            myResult = result;
        }

        @Override
        public K get(int index) {
            return myResult.get(index).extension();
        }

        @Override
        public int size() {
            return myResult.size();
        }
    }

    private final String myApiClassName;
    private @Nullable Class<T> myApiClass = null;

    private final ComponentManager myComponentManager;
    private final Runnable myCheckCanceled;
    private final ComponentScope myComponentScope;
    private final Supplier<ComponentManager> myApplicationGetter;

    private @Nullable Map<Class, Object> myInstanceOfCacheValue = null;
    private @Nullable Map<ExtensionPointCacheKey, Object> myCaches = null;
    private long myModificationCount;
    private volatile CacheValue<T> myCacheValue;
    private volatile @Nullable Boolean myHasAnyExtension = null;

    @SuppressWarnings("unchecked")
    public NewExtensionPointImpl(
        String apiClassName,
        List<InjectingBinding> bindings,
        ComponentManager componentManager,
        Runnable checkCanceled,
        ComponentScope componentScope,
        Supplier<ComponentManager> applicationGetter
    ) {
        myApiClassName = apiClassName;
        myCheckCanceled = checkCanceled;
        myComponentManager = componentManager;
        myComponentScope = componentScope;
        myApplicationGetter = applicationGetter;
        myCacheValue = new CacheValue<>(null, bindings);
    }

    public void initIfNeed(Class<T> apiClass) {
        if (myApiClass != null) {
            return;
        }

        myApiClass = apiClass;
    }

    public void reset(List<InjectingBinding> newBindings) {
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

    @Override
    public List<T> sort(List<T> extensionsList) {
        return sortImpl(extensionsList);
    }

    @SuppressWarnings("unchecked")
    private static <K> List<K> sortImpl(List<K> extensionsList) {
        List<LoadingOrder.Orderable> orders = new ArrayList<>(extensionsList.size());
        for (K extension : extensionsList) {
            PluginDescriptor plugin = PluginManager.getPlugin(extension.getClass());
            if (plugin == null) {
                orders.add(new InvalidOrderable<>(extension));
            }
            else {
                orders.add(new ExtensionImplOrderable<>(new ExtensionValue<>(extension, plugin)));
            }
        }
        LoadingOrder.sort(orders);
        return orders.stream().map(orderable -> (K) orderable.getObjectValue()).toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends T> @Nullable K findExtension(Class<K> extensionClass) {
        Map<Class, Object> instanceOfCacheValue = myInstanceOfCacheValue;
        if (instanceOfCacheValue == null) {
            instanceOfCacheValue = myInstanceOfCacheValue = Maps.newConcurrentHashMap(HashingStrategy.identity());
        }

        Object result = instanceOfCacheValue.computeIfAbsent(
            extensionClass,
            aClass -> {
                K instance = ContainerUtil.findInstance(getExtensionList(), extensionClass);
                return instance == null ? ObjectUtil.NULL : instance;
            }
        );

        return result == ObjectUtil.NULL ? null : (K) result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> K getOrBuildCache(ExtensionPointCacheKey<T, K> key) {
        Map<ExtensionPointCacheKey, Object> caches = myCaches;
        if (caches == null) {
            caches = myCaches = Maps.newConcurrentHashMap(HashingStrategy.identity());
        }

        return (K) caches.computeIfAbsent(key, k -> key.getFactory().apply(new ExtensionWalker<>() {
            @Override
            public void walk(Consumer<T> consumer) {
                forEachExtensionSafe(consumer);
            }

            @Override
            public Function<List<T>, List<T>> sorter() {
                return NewExtensionPointImpl::sortImpl;
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private List<ExtensionValue<T>> build(@Nullable List<InjectingBinding> injectingBindings) {
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

        List<ExtensionValue<T>> extensions = new ArrayList<>(injectingBindings.size());
        for (InjectingBinding binding : injectingBindings) {
            if (!InjectingBindingHolderImpl.isValid(binding, myComponentManager.getProfiles())) {
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

                extension =
                    (T) injectingContainer.getUnbindedInstance(binding.getImplClass(), binding.getParameterTypes(), binding::create);

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

                PluginDescriptor plugin = Objects.requireNonNull(PluginManager.getPlugin(binding.getImplClass()));
                ExtensionValue<T> extensionValue = new ExtensionValue(extension, plugin);
                if (extensions.contains(extensionValue)) {
                    LOG.error("Extension " + extension.getClass() + " duplicated. Extension: " + getName());
                    continue;
                }

                extensions.add(extensionValue);
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
            //noinspection GetExtensionPoint
            myApplicationGetter.get().getExtensionPoint(ExtensionExtender.class).forEach(extender -> {
                if (extender.getExtensionClass() == apiClass) {
                    PluginDescriptor descriptor = Objects.requireNonNull(PluginManager.getPlugin(extender.getClass()));

                    if (extender.hasAnyExtensions(myComponentManager)) {
                        extender.extend(myComponentManager, it -> extensions.add(new ExtensionValue(it, descriptor)));
                    }
                }
            });
        }

        PluginDescriptor apiPlugin = Objects.requireNonNull(PluginManager.getPlugin(apiClass));

        // prepare for sorting
        List<ExtensionImplOrderable<T>> orders = new ArrayList<>(extensions.size());
        for (ExtensionValue<T> extension : extensions) {
            ExtensionImplOrderable<T> orderable = new ExtensionImplOrderable<>(extension);

            orderable.reportFirstLastRestriction(apiPlugin);

            orders.add(orderable);
        }

        LoadingOrder.sort(orders);

        // set new order
        for (int i = 0; i < orders.size(); i++) {
            ExtensionImplOrderable<T> order = orders.get(i);
            extensions.set(i, order.getValue());
        }

        return List.of(extensions.toArray(ExtensionValue[]::new));
    }

    private List<ExtensionValue<T>> buildOrGet() {
        CacheValue<T> cacheValue = myCacheValue;

        List<ExtensionValue<T>> extensionCache = cacheValue.myExtensionCache;
        if (extensionCache == null) {
            List<ExtensionValue<T>> result = build(cacheValue.myInjectingBindings);
            CacheValue<T> value = set(result);
            extensionCache = Objects.requireNonNull(value.myExtensionCache);
        }
        return extensionCache;
    }

    @Override
    public @Nullable T findFirstSafe(@InheritCallerContext Predicate<T> predicate) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).extension();
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

    @Override
    public <R extends @Nullable Object> @Nullable R computeSafeIfAny(@InheritCallerContext Function<? super T, ? extends R> processor) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).extension();
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

    @Override
    public <R extends @Nullable Object, CR extends Collection<? super R>>
    CR collectMapped(CR results, @InheritCallerContext Function<? super T, ? extends R> processor) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).extension();
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

    @Override
    public <K extends @Nullable Object, V extends @Nullable Object, M extends Map<? super K, ? super V>> M collectMapped(
        M results,
        @InheritCallerContext Function<? super T, ? extends K> keyMapper,
        @InheritCallerContext Function<? super T, ? extends V> valueMapper
    ) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).extension();
            try {
                K key = keyMapper.apply(t);
                V value = valueMapper.apply(t);
                if (key != null && value != null) {
                    results.put(key, value);
                }
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
        return results;
    }

    @Override
    public <CE extends Collection<T>> CE collectFiltered(CE results, @InheritCallerContext Predicate<? super T> predicate) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).extension();
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
    public void forEachExtensionSafe(@InheritCallerContext Consumer<? super T> consumer) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).extension();
            try {
                consumer.accept(t);
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, t);
            }
        }
    }

    @Override
    public void forEachBreakable(@InheritCallerContext Function<? super T, Flow> breakableConsumer) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = extensionCache.size(); i < n; i++) {
            T t = extensionCache.get(i).extension();
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
    public void processWithPluginDescriptor(@InheritCallerContext BiConsumer<? super T, ? super PluginDescriptor> consumer) {
        List<ExtensionValue<T>> extensionCache = buildOrGet();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < extensionCache.size(); i++) {
            ExtensionValue<T> extensionValue = extensionCache.get(i);
            try {
                consumer.accept(extensionValue.extension(), extensionValue.pluginDescriptor());
            }
            catch (Throwable e) {
                ExtensionLogger.checkException(e, extensionValue.extension());
            }
        }
    }

    public CacheValue<T> set(List<ExtensionValue<T>> list) {
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
            @SuppressWarnings("GetExtensionPoint")
            boolean hasAnyExtension = myApplicationGetter.get().getExtensionPoint(ExtensionExtender.class).anyMatchSafe(
                extender -> extender.getExtensionClass() == extensionClass && extender.hasAnyExtensions(myComponentManager)
            );
            if (hasAnyExtension) {
                return myHasAnyExtension = true;
            }
        }

        // at this moment myExtensionAdapters must exists
        return myHasAnyExtension = !Objects.requireNonNull(cacheValue.myInjectingBindings).isEmpty();
    }

    @Override
    public long getModificationCount() {
        return myModificationCount;
    }

    @Override
    public List<T> getExtensionList() {
        CacheValue<T> cacheValue = myCacheValue;

        List<T> extensionCache = cacheValue.myUnwrapExtensionCache;
        if (extensionCache != null) {
            return extensionCache;
        }

        List<ExtensionValue<T>> result = build(cacheValue.myInjectingBindings);
        CacheValue<T> value = set(result);
        return Objects.requireNonNull(value.myUnwrapExtensionCache);
    }

    @Override
    public String getClassName() {
        return myApiClassName;
    }

    @Override
    public Class<T> getExtensionClass() {
        return Objects.requireNonNull(myApiClass, "apiClass not initialized");
    }
}
