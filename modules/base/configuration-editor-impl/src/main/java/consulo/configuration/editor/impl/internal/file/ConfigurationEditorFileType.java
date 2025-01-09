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

import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.configuration.editor.internal.ConfigurationEditorFileReference;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
public class ConfigurationEditorFileType implements FileType, ConfigurationEditorFileReference {
    private final ConfigurationFileEditorProvider myProvider;

    public ConfigurationEditorFileType(ConfigurationFileEditorProvider provider) {
        myProvider = provider;
    }

    @Nonnull
    @Override
    public ConfigurationFileEditorProvider getProvider() {
        return myProvider;
    }

    @Nonnull
    @Override
    public String getId() {
        return myProvider.getId();
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.of();
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return myProvider.getIcon();
    }
}
