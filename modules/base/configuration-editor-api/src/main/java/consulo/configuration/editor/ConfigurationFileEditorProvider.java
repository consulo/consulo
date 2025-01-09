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
package consulo.configuration.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ConfigurationFileEditorProvider {
    @Nonnull
    String getId();

    @Nonnull
    Image getIcon();

    @Nonnull
    LocalizeValue getName();

    @Nullable
    default ColorValue getColor() {
        return null;
    }

    @Nonnull
    ConfigurationFileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file);
}
