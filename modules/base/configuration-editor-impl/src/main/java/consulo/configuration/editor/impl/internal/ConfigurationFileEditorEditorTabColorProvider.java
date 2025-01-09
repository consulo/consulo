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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.configuration.editor.impl.internal.file.ConfigurationEditorFile;
import consulo.fileEditor.EditorTabColorProvider;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
@ExtensionImpl
public class ConfigurationFileEditorEditorTabColorProvider implements EditorTabColorProvider, DumbAware {
    @Nullable
    @Override
    public Color getEditorTabColor(Project project, VirtualFile file) {
        if (file instanceof ConfigurationEditorFile configurationEditorFile) {
            ConfigurationFileEditorProvider provider = configurationEditorFile.getProvider();

            ColorValue color = provider.getColor();
            if (color != null) {
                return TargetAWT.to(color);
            }
        }
        return null;
    }
}
