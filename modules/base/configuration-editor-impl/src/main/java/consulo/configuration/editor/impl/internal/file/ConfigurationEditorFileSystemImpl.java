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
package consulo.configuration.editor.impl.internal.file;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.HiddenFileSystem;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
@ExtensionImpl
public class ConfigurationEditorFileSystemImpl extends BaseVirtualFileSystem implements
    NonPhysicalFileSystem,
    HiddenFileSystem {

    public static final String PROTOCOL = "configuration_editor";

    private final Application myApplication;

    @Inject
    public ConfigurationEditorFileSystemImpl(Application application) {
        myApplication = application;
    }

    @Nonnull
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Nullable
    @Override
    public VirtualFile findFileByPath(@Nonnull String path) {
        if (!path.isBlank() && path.charAt(0) == '/') {
            String extensioId = path.substring(1, path.length());

            ConfigurationFileEditorProvider editorProvider = myApplication
                .getExtensionPoint(ConfigurationFileEditorProvider.class)
                .findFirstSafe(provider -> Objects.equals(extensioId, provider.getId()));

            if (editorProvider != null) {
                return new ConfigurationEditorFile(new ConfigurationEditorFileType(editorProvider), this);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
        return findFileByPath(path);
    }

    @Override
    public void refresh(boolean asynchronous) {
    }
}
