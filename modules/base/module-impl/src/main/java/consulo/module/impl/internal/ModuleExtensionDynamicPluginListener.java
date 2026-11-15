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
package consulo.module.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.lang.EmptyRunnable;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Module extensions of plugins unloaded at runtime leave every module layer at once, their state is kept and read back when
 * the plugins load again. Each module changes its roots once per session.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@TopicImpl(ComponentScope.APPLICATION)
public class ModuleExtensionDynamicPluginListener implements DynamicPluginListener {
    private final Application myApplication;

    @Inject
    public ModuleExtensionDynamicPluginListener(Application application) {
        myApplication = application;
    }

    @Override
    public void beforePluginsChange(DynamicPluginChange change) {
        if (change.getUnloading().isEmpty()) {
            return;
        }

        List<ClassLoader> classLoaders = new ArrayList<>();
        for (PluginDescriptor plugin : change.getUnloading()) {
            classLoaders.add(plugin.getPluginClassLoader());
        }

        forEachModule(layer -> {
            boolean changed = false;
            for (ClassLoader classLoader : classLoaders) {
                changed |= layer.unloadPluginExtensions(classLoader);
            }
            return changed;
        });
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        if (!change.getLoading().isEmpty()) {
            forEachModule(ModuleRootLayerImpl::loadPluginExtensions);
        }
    }

    /**
     * @param action returns whether the roots of the module changed
     */
    private void forEachModule(Predicate<ModuleRootLayerImpl> action) {
        myApplication.runWriteAction(() -> {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                if (project.isDisposed()) {
                    continue;
                }

                for (Module module : ModuleManager.getInstance(project).getModules()) {
                    ModuleRootManagerImpl rootManager = (ModuleRootManagerImpl) ModuleRootManager.getInstance(module);
                    boolean changed = false;
                    for (ModuleRootLayer layer : rootManager.getLayers().values()) {
                        changed |= action.test((ModuleRootLayerImpl) layer);
                    }
                    if (changed) {
                        rootManager.makeRootsChange(EmptyRunnable.getInstance());
                    }
                }
            }
        });
    }
}
