/*
 * Copyright 2013-2025 consulo.io
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
package consulo.configuration.editor.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.configuration.editor.impl.internal.file.ConfigurationEditorFile;
import consulo.configuration.editor.impl.internal.file.ConfigurationEditorFileSystemImpl;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
@Singleton
@ServiceImpl
public class ConfigurationFileEditorManagerImpl implements ConfigurationFileEditorManager {
    private final Application myApplication;
    private final VirtualFileManager myVirtualFileManager;

    @Inject
    public ConfigurationFileEditorManagerImpl(Application application, VirtualFileManager virtualFileManager) {
        myApplication = application;
        myVirtualFileManager = virtualFileManager;
    }

    @RequiredUIAccess
    @Override
    public void open(@Nonnull Project project, @Nonnull Class<? extends ConfigurationFileEditorProvider> providerClass) {
        open(project, myApplication.getExtensionPoint(ConfigurationFileEditorProvider.class).findExtensionOrFail(providerClass));
    }

    @RequiredUIAccess
    @Override
    public void open(@Nonnull Project project, @Nonnull ConfigurationFileEditorProvider provider) {
        VirtualFileSystem fileSystem = myVirtualFileManager.getFileSystem(ConfigurationEditorFileSystemImpl.PROTOCOL);
        if (!(fileSystem instanceof ConfigurationEditorFileSystemImpl fs)) {
            return;
        }

        VirtualFile file = fs.findFileByPath("/" + provider.getId());
        if (file instanceof ConfigurationEditorFile configurationEditorFile) {
            FileEditorManager.getInstance(project).openFile(configurationEditorFile, true);
        }
    }
}
