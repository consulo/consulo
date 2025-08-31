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

import consulo.annotation.access.RequiredReadAction;
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
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2013-11-12
 */
@Singleton
@ServiceImpl
public class ModuleExtensionHelperImpl implements ModuleExtensionHelper, Disposable {
    private final Project myProject;

    private volatile MultiMap<Class<? extends ModuleExtension>, ModuleExtension> myExtensions;

    @Inject
    public ModuleExtensionHelperImpl(Project project) {
        myProject = project;

        project.getMessageBus().connect(this).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void beforeRootsChange(ModuleRootEvent event) {
                myExtensions = null;
            }
        });
    }

    @Override
    public boolean hasModuleExtension(@Nonnull Class<? extends ModuleExtension> clazz) {
        checkInit();

        assert myExtensions != null;

        return !getModuleExtensions(clazz).isEmpty();
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(@Nonnull Class<T> clazz) {
        checkInit();

        assert myExtensions != null;

        Collection<ModuleExtension> moduleExtensions = myExtensions.get(clazz);
        if (moduleExtensions.isEmpty()) {
            for (Map.Entry<Class<? extends ModuleExtension>, Collection<ModuleExtension>> entry : myExtensions.entrySet()) {
                Class<? extends ModuleExtension> targetCheck = entry.getKey();

                if (clazz.isAssignableFrom(targetCheck)) {
                    myExtensions.put(clazz, moduleExtensions = entry.getValue());
                    break;
                }
            }
        }
        return (Collection) moduleExtensions;
    }

    @Nonnull
    @Override
    public String getModuleExtensionName(@Nonnull ModuleExtension<?> moduleExtension) {
        ModuleExtensionProvider provider = ModuleExtensionProvider.findProvider(moduleExtension.getId());
        assert provider != null;
        return provider.getName().getValue();
    }

    @Nullable
    @Override
    public Image getModuleExtensionIcon(@Nonnull String extensionId) {
        ModuleExtensionProvider provider = ModuleExtensionProvider.findProvider(extensionId);
        return provider == null ? null : provider.getIcon();
    }

    @RequiredReadAction
    private void checkInit() {
        if (myExtensions == null) {
            myExtensions = MultiMap.createConcurrentSet();
            for (Module o : ModuleManager.getInstance(myProject).getModules()) {
                for (ModuleExtension moduleExtension : ModuleRootManager.getInstance(o).getExtensions()) {
                    myExtensions.putValue(moduleExtension.getClass(), moduleExtension);
                }
            }
        }
    }

    @Override
    public void dispose() {
        myExtensions = null;
    }
}
