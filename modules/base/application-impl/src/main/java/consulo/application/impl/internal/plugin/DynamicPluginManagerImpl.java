/*
 * Copyright 2013-2026 consulo.io
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
package consulo.application.impl.internal.plugin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.impl.internal.util.CachedValueStabilityChecker;
import consulo.application.internal.MessagePool;
import consulo.application.internal.plugin.DynamicPluginManager;
import consulo.application.internal.plugin.DynamicPluginSession;
import consulo.application.internal.plugin.DynamicPluginSessionResult;
import consulo.application.internal.plugin.PluginsLoader;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.bind.InjectingBinding;
import consulo.component.impl.internal.BaseComponentManager;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingBindingHolder;
import consulo.component.internal.inject.InjectingBindingLoader;
import consulo.component.internal.inject.NewBindingLoader;
import consulo.component.internal.inject.NewInjectingBindingCollector;
import consulo.component.internal.inject.NewTopicBindingCollector;
import consulo.component.store.internal.StorableComponent;
import consulo.component.util.localize.AbstractBundle;
import consulo.disposer.internal.DisposerInternal;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.localize.LocalizeManager;
import consulo.localize.internal.LocalizeManagerEx;
import consulo.logging.Logger;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-11-12
 */
@Singleton
@ServiceImpl
public class DynamicPluginManagerImpl implements DynamicPluginManager {
    private static final Logger LOG = Logger.getInstance(DynamicPluginManagerImpl.class);

    private final Application myApplication;
    private final ComponentBinding myComponentBinding;

    @Inject
    public DynamicPluginManagerImpl(Application application, ComponentBinding componentBinding) {
        myApplication = application;
        myComponentBinding = componentBinding;
    }

    @Override
    public boolean isEnabled() {
        return EarlyAccessProgramManager.is(DynamicPluginLoadingEapDescriptor.class);
    }

    @Override
    public DynamicPluginSession createSession() {
        return new DynamicPluginSessionImpl(this);
    }

    synchronized DynamicPluginSessionResult apply(List<Path> installPaths, Set<PluginId> uninstallIds) {
        Set<PluginId> restartRequired = new LinkedHashSet<>();
        Set<PluginId> uninstalled = new LinkedHashSet<>();
        List<PluginDescriptor> loaded = new ArrayList<>();

        Map<PluginId, PluginDescriptor> installs = new LinkedHashMap<>();
        for (Path path : installPaths) {
            PluginDescriptor onDisk = PluginsLoader.loadPluginDescriptor(path.toFile());
            if (onDisk == null) {
                LOG.warn("Can't read plugin descriptor: " + path);
                continue;
            }

            PluginDescriptor existing = PluginManager.findPlugin(onDisk.getPluginId());
            if (existing != null && existing.getStatus() == PluginDescriptorStatus.OK) {
                LOG.info("Plugin " + onDisk.getPluginId() + " is already loaded, restart required");
                restartRequired.add(onDisk.getPluginId());
                continue;
            }
            installs.put(onDisk.getPluginId(), onDisk);
        }

        Set<PluginId> uninstalls = new LinkedHashSet<>();
        for (PluginId pluginId : uninstallIds) {
            PluginDescriptor descriptor = PluginManager.findPlugin(pluginId);
            if (descriptor == null) {
                continue;
            }

            if (descriptor.getStatus() == PluginDescriptorStatus.OK) {
                uninstalls.add(pluginId);
            }
            else if (descriptor.getNioPath() != null) {
                PluginsLoader.unregisterRuntimePlugin(descriptor);
                PluginsLoader.deleteRuntimePlugin(descriptor.getNioPath());
                uninstalled.add(pluginId);
            }
            else {
                restartRequired.add(pluginId);
            }
        }

        List<PluginDescriptor> toUnload = planUnload(uninstalls, installs, restartRequired);

        Set<PluginId> unloadingIds = new HashSet<>();
        for (PluginDescriptor plugin : toUnload) {
            unloadingIds.add(plugin.getPluginId());
        }

        Map<PluginId, PluginDescriptor> candidates = new LinkedHashMap<>(installs);
        for (PluginDescriptor plugin : toUnload) {
            if (!uninstalls.contains(plugin.getPluginId())) {
                candidates.put(plugin.getPluginId(), plugin);
            }
        }
        for (PluginDescriptor plugin : PluginManager.getPlugins()) {
            if (plugin.getStatus() == PluginDescriptorStatus.DEPENDENCY_NOT_LOADED
                && plugin.getNioPath() != null
                && !uninstallIds.contains(plugin.getPluginId())) {
                candidates.putIfAbsent(plugin.getPluginId(), plugin);
            }
        }

        Set<PluginId> available = new HashSet<>();
        for (PluginDescriptor plugin : PluginManager.getPlugins()) {
            if (plugin.getStatus() == PluginDescriptorStatus.OK && !unloadingIds.contains(plugin.getPluginId())) {
                available.add(plugin.getPluginId());
            }
        }

        List<PluginDescriptor> loadOrder = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (PluginDescriptor candidate : candidates.values()) {
                if (!available.contains(candidate.getPluginId()) && requiresOnly(candidate, available)) {
                    loadOrder.add(candidate);
                    available.add(candidate.getPluginId());
                    progress = true;
                }
            }
        }

        List<PluginDescriptor> unloading = new ArrayList<>(toUnload);
        Collections.reverse(unloading);
        DynamicPluginChange change = new DynamicPluginChangeImpl(unloading, loadOrder);
        if (unloading.isEmpty() && loadOrder.isEmpty() && uninstalls.isEmpty() && installs.isEmpty()) {
            return new DynamicPluginSessionResultImpl(loaded, uninstalled, restartRequired);
        }

        if (!unloading.isEmpty()) {
            myApplication.saveSettings();
        }

        List<ClassLoader> unloadedClassLoaders = new ArrayList<>();
        for (PluginDescriptor plugin : unloading) {
            unloadedClassLoaders.add(plugin.getPluginClassLoader());
        }

        DynamicPluginListener publisher = myApplication.getMessageBus().syncPublisher(DynamicPluginListener.class);
        publisher.beforePluginsChange(change);
        try {
            Map<PluginId, Path> paths = new HashMap<>();
            for (PluginDescriptor plugin : candidates.values()) {
                paths.put(plugin.getPluginId(), Objects.requireNonNull(plugin.getNioPath()));
            }
            for (PluginDescriptor plugin : toUnload) {
                paths.put(plugin.getPluginId(), Objects.requireNonNull(plugin.getNioPath()));
            }

            for (PluginDescriptor plugin : unloading) {
                doUnload(plugin);
            }

            for (PluginId pluginId : uninstalls) {
                PluginsLoader.deleteRuntimePlugin(paths.get(pluginId));
                uninstalled.add(pluginId);
            }

            for (PluginDescriptor candidate : candidates.values()) {
                if (!loadOrder.contains(candidate) && !installs.containsKey(candidate.getPluginId())
                    && candidate.getStatus() == PluginDescriptorStatus.OK) {
                    PluginsLoader.registerNotLoadedPlugin(paths.get(candidate.getPluginId()));
                }
            }

            for (PluginDescriptor plugin : installs.values()) {
                if (!loadOrder.contains(plugin)) {
                    PluginsLoader.registerNotLoadedPlugin(paths.get(plugin.getPluginId()));
                }
            }

            for (PluginDescriptor plugin : loadOrder) {
                PluginId pluginId = plugin.getPluginId();
                Path path = paths.get(pluginId);
                PluginDescriptor descriptor = doLoad(path);
                if (descriptor != null) {
                    if (installs.containsKey(pluginId)) {
                        loaded.add(descriptor);
                    }
                }
                else if (installs.containsKey(pluginId)) {
                    restartRequired.add(pluginId);
                }
                else {
                    PluginsLoader.registerNotLoadedPlugin(path);
                }
            }
        }
        finally {
            publisher.pluginsChanged(change);

            // later unloads and the listeners may have cached classes of plugins unloaded earlier in the session
            for (ClassLoader classLoader : unloadedClassLoaders) {
                XmlSerializer.dropCachedClasses(classLoader);
            }
            CachedValueStabilityChecker.cleanupFieldCache();
            if (!unloadedClassLoaders.isEmpty()) {
                // reported errors keep the stack traces of plugin classes
                MessagePool.getInstance().clearFatals();
            }
        }
        return new DynamicPluginSessionResultImpl(loaded, uninstalled, restartRequired);
    }

    private @Nullable PluginDescriptor doLoad(Path pluginPath) {
        PluginDescriptor descriptor = PluginsLoader.prepareRuntimePlugin(pluginPath);
        if (descriptor == null) {
            return null;
        }

        NewInjectingBindingCollector injectingCollector = new NewInjectingBindingCollector();
        NewTopicBindingCollector topicCollector = new NewTopicBindingCollector();

        try {
            new NewBindingLoader(injectingCollector, topicCollector).loadPlugin(descriptor);
        }
        catch (Throwable e) {
            LOG.error("Fail to read bindings of plugin " + descriptor.getPluginId(), e);
            PluginsLoader.discardRuntimePlugin(descriptor);
            return null;
        }

        String restartReason = getRestartReason(injectingCollector);
        if (restartReason != null) {
            LOG.info("Plugin " + descriptor.getPluginId() + " " + restartReason + ", restart required");
            PluginsLoader.discardRuntimePlugin(descriptor);
            return null;
        }

        PluginsLoader.registerRuntimePlugin(descriptor);

        myComponentBinding.injectingBindingLoader().addBindings(injectingCollector);
        myComponentBinding.topicBindingLoader().addBindings(topicCollector.getBindings());

        ((LocalizeManagerEx) LocalizeManager.get()).loadPlugin(Objects.requireNonNull(descriptor.getNioPath()));
        ((BaseIconLibraryManager) IconLibraryManager.get()).loadPlugin(descriptor);

        List<BaseComponentManager> managers = BaseComponentManager.getLiveManagers();
        Map<BaseComponentManager, List<Class>> notLazyServices = new LinkedHashMap<>();
        for (BaseComponentManager manager : managers) {
            ComponentScope scope = manager.getComponentScope();

            notLazyServices.put(manager, manager.bindPluginServices(getBindings(injectingCollector.getServices(), scope)));
            manager.topicBindingsAdded(getBindings(injectingCollector.getTopics(), scope));
            manager.extensionBindingsChanged(getBindings(injectingCollector.getExtensions(), scope).keySet(), null);
        }

        for (Map.Entry<BaseComponentManager, List<Class>> entry : notLazyServices.entrySet()) {
            entry.getKey().initPluginServices(entry.getValue());
        }

        LOG.info("Plugin " + descriptor.getPluginId() + " loaded without restart");

        myApplication.getMessageBus().syncPublisher(DynamicPluginListener.class).pluginLoaded(descriptor);
        return descriptor;
    }

    /**
     * Loaded plugins to unload: the uninstalled ones, the ones with an optional dependency on an installed one (they
     * reload to pick it up) and their dependents. A requested change whose plugins can't be unloaded moves to the
     * restart set.
     *
     * @return in load order
     */
    private List<PluginDescriptor> planUnload(
        Set<PluginId> uninstalls,
        Map<PluginId, PluginDescriptor> installs,
        Set<PluginId> restartRequired
    ) {
        while (true) {
            List<PluginDescriptor> toUnload = collectUnload(uninstalls, installs.keySet());

            Set<PluginId> blocked = new HashSet<>();
            for (PluginDescriptor plugin : toUnload) {
                String restartReason = getUnloadRestartReason(plugin, toUnload);
                if (restartReason != null) {
                    LOG.info("Plugin " + plugin.getPluginId() + " " + restartReason + ", restart required");
                    blocked.add(plugin.getPluginId());
                }
            }

            if (blocked.isEmpty()) {
                return toUnload;
            }

            int requested = uninstalls.size() + installs.size();
            for (Iterator<PluginId> it = uninstalls.iterator(); it.hasNext(); ) {
                PluginId pluginId = it.next();
                if (containsAny(collectUnload(Set.of(pluginId), Set.of()), blocked)) {
                    restartRequired.add(pluginId);
                    it.remove();
                }
            }

            for (Iterator<PluginId> it = installs.keySet().iterator(); it.hasNext(); ) {
                PluginId pluginId = it.next();
                if (containsAny(collectUnload(Set.of(), Set.of(pluginId)), blocked)) {
                    restartRequired.add(pluginId);
                    it.remove();
                }
            }

            if (uninstalls.size() + installs.size() == requested) {
                restartRequired.addAll(uninstalls);
                restartRequired.addAll(installs.keySet());
                uninstalls.clear();
                installs.clear();
            }
        }
    }

    private static List<PluginDescriptor> collectUnload(Set<PluginId> uninstalls, Set<PluginId> installs) {
        Set<PluginId> roots = new HashSet<>(uninstalls);
        for (PluginDescriptor plugin : PluginManager.getPlugins()) {
            if (plugin.getStatus() == PluginDescriptorStatus.OK
                && containsAny(Arrays.asList(plugin.getOptionalDependentPluginIds()), installs)) {
                roots.add(plugin.getPluginId());
            }
        }

        List<PluginDescriptor> result = new ArrayList<>();
        for (PluginId pluginId : roots) {
            PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
            if (plugin != null && plugin.getStatus() == PluginDescriptorStatus.OK) {
                result.add(plugin);
            }
        }
        result.addAll(collectDependents(roots));
        result.sort(Comparator.comparingInt(plugin -> PluginsLoader.getLoadOrder(plugin.getPluginId())));
        return result;
    }

    private static boolean containsAny(Collection<?> values, Collection<?> wanted) {
        for (Object value : values) {
            if (value instanceof PluginDescriptor plugin ? wanted.contains(plugin.getPluginId()) : wanted.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether every required dependency of the plugin is among the given ones
     */
    private static boolean requiresOnly(PluginDescriptor plugin, Set<PluginId> available) {
        Set<PluginId> optionalIds = Set.of(plugin.getOptionalDependentPluginIds());
        for (PluginId dependencyId : plugin.getDependentPluginIds()) {
            if (!optionalIds.contains(dependencyId) && !available.contains(dependencyId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Loaded plugins which depend on the given ones, directly or through others.
     */
    private static List<PluginDescriptor> collectDependents(Set<PluginId> pluginIds) {
        Set<PluginId> ids = new HashSet<>(pluginIds);
        List<PluginDescriptor> result = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (PluginDescriptor plugin : PluginManager.getPlugins()) {
                if (plugin.getStatus() == PluginDescriptorStatus.OK
                    && !ids.contains(plugin.getPluginId())
                    && containsAny(Arrays.asList(plugin.getDependentPluginIds()), ids)) {
                    ids.add(plugin.getPluginId());
                    result.add(plugin);
                    progress = true;
                }
            }
        }
        return result;
    }

    private void doUnload(PluginDescriptor descriptor) {
        PluginId pluginId = descriptor.getPluginId();
        ClassLoader classLoader = descriptor.getPluginClassLoader();
        List<BaseComponentManager> managers = new ArrayList<>(BaseComponentManager.getLiveManagers());
        Collections.reverse(managers);

        myApplication.getMessageBus().syncPublisher(DynamicPluginListener.class).beforePluginUnload(descriptor);

        // a write action cancels read actions which may still call into the plugin
        myApplication.runWriteAction(() -> {
            InjectingBindingLoader removed = myComponentBinding.injectingBindingLoader().removeBindings(classLoader);
            myComponentBinding.topicBindingLoader().removeBindings(classLoader);

            for (BaseComponentManager manager : managers) {
                if (manager instanceof StorableComponent storable) {
                    storable.getStateStore().unloadComponents(classLoader);
                }

                manager.unloadPlugin(classLoader);
                manager.extensionBindingsChanged(getBindings(removed.extensions(), manager.getComponentScope()).keySet(), classLoader);
            }

            DisposerInternal.ourInstance.disposeObjectsOf(classLoader);
        });

        Path pluginPath = Objects.requireNonNull(descriptor.getNioPath());
        ((LocalizeManagerEx) LocalizeManager.get()).unloadPlugin(pluginPath);

        PluginsLoader.unregisterRuntimePlugin(descriptor);

        myApplication.getMessageBus().syncPublisher(DynamicPluginListener.class).pluginUnloaded(descriptor);

        XmlSerializer.dropCachedClasses(classLoader);
        CachedValueStabilityChecker.cleanupFieldCache();
        AbstractBundle.dropCachedBundles(classLoader);

        WeakReference<ClassLoader> classLoaderRef = new WeakReference<>(classLoader);
        PluginsLoader.discardRuntimePlugin(descriptor);

        LOG.info("Plugin " + pluginId + " unloaded without restart");

        AppExecutorUtil.getAppExecutorService().execute(() -> checkClassLoaderCollected(pluginId, classLoaderRef));
    }

    /**
     * @param unloadedTogether plugins unloaded at the same time, their dependency on this plugin does not block
     */
    private @Nullable String getUnloadRestartReason(PluginDescriptor descriptor, Collection<PluginDescriptor> unloadedTogether) {
        PluginId pluginId = descriptor.getPluginId();
        if (PluginIds.isPlatformPlugin(pluginId) || descriptor.getStatus() != PluginDescriptorStatus.OK) {
            return "is not a loaded plugin";
        }

        for (PluginDescriptor plugin : PluginManager.getPlugins()) {
            if (plugin != descriptor
                && plugin.getStatus() == PluginDescriptorStatus.OK
                && !unloadedTogether.contains(plugin)
                && Arrays.asList(plugin.getDependentPluginIds()).contains(pluginId)) {
                return "is a dependency of " + plugin.getPluginId();
            }
        }

        ClassLoader classLoader = descriptor.getPluginClassLoader();
        InjectingBindingLoader bindings = myComponentBinding.injectingBindingLoader();
        for (InjectingBindingHolder holder : bindings.services().values()) {
            for (List<InjectingBinding> list : holder.getBindings().values()) {
                for (InjectingBinding binding : list) {
                    if (binding.getImplClass().getClassLoader() == classLoader && binding.getApiClass().getClassLoader() != classLoader) {
                        return "implements service " + binding.getApiClassName();
                    }
                }
            }
        }
        return null;
    }

    private static void checkClassLoaderCollected(PluginId pluginId, WeakReference<ClassLoader> classLoaderRef) {
        // UI which showed the plugin, like an open Plugins page, releases it later
        for (int i = 0; i < 20 && classLoaderRef.get() != null; i++) {
            System.gc();
            try {
                Thread.sleep(i < 10 ? 500 : 10_000);
            }
            catch (InterruptedException e) {
                return;
            }
        }

        if (classLoaderRef.get() == null) {
            LOG.info("Class loader of plugin " + pluginId + " is collected");
        }
        else {
            LOG.warn("Class loader of plugin " + pluginId + " is still referenced after unload");
        }
    }

    private @Nullable String getRestartReason(NewInjectingBindingCollector collector) {
        for (Map.Entry<ComponentScope, InjectingBindingHolder> entry : collector.getServices().entrySet()) {
            Map<String, List<InjectingBinding>> loaded =
                myComponentBinding.injectingBindingLoader().getHolder(ServiceAPI.class, entry.getKey()).getBindings();

            for (String apiClassName : entry.getValue().getBindings().keySet()) {
                if (loaded.containsKey(apiClassName)) {
                    return "overrides service " + apiClassName;
                }
            }
        }

        return null;
    }

    private static Map<String, List<InjectingBinding>> getBindings(Map<ComponentScope, InjectingBindingHolder> holders, ComponentScope scope) {
        InjectingBindingHolder holder = holders.get(scope);
        return holder == null ? Map.of() : holder.getBindings();
    }
}
