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
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.configuration.editor.impl.internal.file.ConfigurationEditorFileImpl;
import consulo.configuration.editor.impl.internal.file.ConfigurationEditorFileSystemImpl;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
    public void open(@Nonnull Project project,
                     @Nonnull Class<? extends ConfigurationFileEditorProvider> providerClass,
                     @Nonnull Map<String, String> params) {
        open(project, myApplication.getExtensionPoint(ConfigurationFileEditorProvider.class).findExtensionOrFail(providerClass), params);
    }

    @RequiredUIAccess
    @Override
    public void open(@Nonnull Project project, @Nonnull ConfigurationFileEditorProvider provider, @Nonnull Map<String, String> params) {
        VirtualFileSystem fileSystem = myVirtualFileManager.getFileSystem(ConfigurationEditorFileSystemImpl.PROTOCOL);
        if (!(fileSystem instanceof ConfigurationEditorFileSystemImpl fs)) {
            return;
        }

        String path = "/" + provider.getId();
        if (!params.isEmpty()) {
            StringBuilder builder = new StringBuilder("?");

            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    builder.append("&");
                } else {
                    first = false;
                }

                builder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            path += builder.toString();
        }

        VirtualFile file = fs.findFileByPath(path);

        if (file instanceof ConfigurationEditorFileImpl configurationEditorFile) {
            FileEditor[] fileEditors = FileEditorManager.getInstance(project).openFile(configurationEditorFile, true);

            for (FileEditor fileEditor : fileEditors) {
                if (fileEditor instanceof ConfigurationFileEditor configurationFileEditor) {
                    configurationFileEditor.onUpdateRequestParams(configurationEditorFile.getRequestedParams());
                }
            }
        }
    }
}
