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
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.language.file.FileTypeManager;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeDetector;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.List;

/**
 * File types of plugins loaded at runtime are registered with the others, which re-detects file types and reindexes;
 * file types of unloaded plugins are unregistered. Each happens once per session, for all its plugins.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicImpl(ComponentScope.APPLICATION)
public class FileTypeDynamicPluginListener implements DynamicPluginListener {
    private final Application myApplication;
    private final Provider<FileTypeManager> myFileTypeManager;

    private long myDetectorsModificationCount;
    private final List<PluginDescriptor> myLoadedPlugins = new ArrayList<>();

    @Inject
    public FileTypeDynamicPluginListener(Application application, Provider<FileTypeManager> fileTypeManager) {
        myApplication = application;
        myFileTypeManager = fileTypeManager;
    }

    @Override
    public void beforePluginsChange(DynamicPluginChange change) {
        myDetectorsModificationCount = myApplication.getExtensionPoint(FileTypeDetector.class).getModificationCount();
        myLoadedPlugins.clear();

        FileTypeManagerImpl fileTypeManager = (FileTypeManagerImpl) myFileTypeManager.get();
        List<FileType> fileTypes = new ArrayList<>();
        for (PluginDescriptor plugin : change.getUnloading()) {
            fileTypes.addAll(fileTypeManager.getStandardFileTypesOf(plugin.getPluginClassLoader()));
        }

        if (!fileTypes.isEmpty()) {
            myApplication.runWriteAction(() -> fileTypeManager.unregisterPluginFileTypes(fileTypes));
        }
    }

    @Override
    public void pluginLoaded(PluginDescriptor pluginDescriptor) {
        myLoadedPlugins.add(pluginDescriptor);
    }

    @Override
    public void pluginUnloaded(PluginDescriptor pluginDescriptor) {
        // a plugin loaded later in the session (the same one reloaded) registers its file attributes again
        FileAttribute.resetRegisteredIds();
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        List<PluginDescriptor> loadedPlugins = List.copyOf(myLoadedPlugins);
        myLoadedPlugins.clear();

        boolean detectorsChanged =
            myApplication.getExtensionPoint(FileTypeDetector.class).getModificationCount() != myDetectorsModificationCount;
        if (loadedPlugins.isEmpty() && !detectorsChanged) {
            return;
        }

        FileTypeManagerImpl fileTypeManager = (FileTypeManagerImpl) myFileTypeManager.get();
        myApplication.runWriteAction(() -> fileTypeManager.registerPluginFileTypes(loadedPlugins, detectorsChanged));
    }
}
