/*
 * Copyright 2013-2016 consulo.io
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
package consulo.module.impl.internal.extension;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2013-11-12
 */
@Singleton
@ServiceImpl
public class ModuleExtensionHelperImpl implements ModuleExtensionHelper, Disposable {
    private record Snapshot(List<ModuleExtension> extensions, Map<Class<?>, List<ModuleExtension>> byType) {
    }

    private final Project myProject;

    private volatile @Nullable Snapshot mySnapshot;

    @Inject
    public ModuleExtensionHelperImpl(Project project) {
        myProject = project;

        project.getMessageBus().connect(this).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void beforeRootsChange(ModuleRootEvent event) {
                mySnapshot = null;
            }

            @Override
            public void rootsChanged(ModuleRootEvent event) {
                mySnapshot = null;
            }
        });
    }

    @Override
    public boolean hasModuleExtension(Class<? extends ModuleExtension> clazz) {
        return !getExtensionsOfType(clazz).isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(Class<T> clazz) {
        return (Collection) getExtensionsOfType(clazz);
    }

    @Override
    public String getModuleExtensionName(ModuleExtension<?> moduleExtension) {
        ModuleExtensionProvider provider = ModuleExtensionProvider.findProvider(moduleExtension.getId());
        assert provider != null;
        return provider.getName().get();
    }

    @Override
    public @Nullable Image getModuleExtensionIcon(String extensionId) {
        ModuleExtensionProvider provider = ModuleExtensionProvider.findProvider(extensionId);
        return provider == null ? null : provider.getIcon();
    }

    private List<ModuleExtension> getExtensionsOfType(Class<?> clazz) {
        Snapshot snapshot = getSnapshot();
        return snapshot.byType().computeIfAbsent(clazz, type -> {
            List<ModuleExtension> result = new ArrayList<>();
            for (ModuleExtension extension : snapshot.extensions()) {
                if (type.isInstance(extension)) {
                    result.add(extension);
                }
            }
            return result.isEmpty() ? List.of() : List.copyOf(result);
        });
    }

    private Snapshot getSnapshot() {
        Snapshot snapshot = mySnapshot;
        if (snapshot == null) {
            List<ModuleExtension> extensions = new ArrayList<>();
            for (Module module : ModuleManager.getInstance(myProject).getModules()) {
                extensions.addAll(ModuleRootManager.getInstance(module).getExtensions());
            }
            snapshot = new Snapshot(extensions, new ConcurrentHashMap<>());
            mySnapshot = snapshot;
        }
        return snapshot;
    }

    @Override
    public void dispose() {
        mySnapshot = null;
    }
}
