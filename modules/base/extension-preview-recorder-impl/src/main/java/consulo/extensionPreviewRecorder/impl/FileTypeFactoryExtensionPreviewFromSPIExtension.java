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
package consulo.extensionPreviewRecorder.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.SPIClassLoaderExtension;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewRecorder;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * We need register target plugins for provide info about supported files
 *
 * @author VISTALL
 * @since 2025-12-27
 */
@ExtensionImpl
public class FileTypeFactoryExtensionPreviewFromSPIExtension implements ExtensionPreviewRecorder<FileTypeFactory> {
    private final Application myApplication;

    @Inject
    public FileTypeFactoryExtensionPreviewFromSPIExtension(Application application) {
        myApplication = application;
    }

    @Override
    public void analyze(@Nonnull Consumer<ExtensionPreview> recorder) {
        PluginId fileTypeFactoryPluginId = PluginManager.getPlugin(FileTypeFactory.class).getPluginId();

        myApplication.getExtensionPoint(SPIClassLoaderExtension.class).forEach(e -> {
            Set<String> supportedFileExtensions = e.getSupportedFileExtensions();
            if (supportedFileExtensions.isEmpty()) {
                return;
            }

            PluginId implPluginId = Objects.requireNonNull(PluginManager.getPluginId(e.getClass()));

            for (String extension : supportedFileExtensions) {
                recorder.accept(new ExtensionPreview(fileTypeFactoryPluginId, FileTypeFactory.class.getName(), implPluginId, "*|" + extension));
            }
        });
    }
}
