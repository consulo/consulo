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
package consulo.execution.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
@ExtensionImpl
public class RunConfigurationEditorProvider implements ConfigurationFileEditorProvider {
    public static final String RUN_CONFIGURATION_ID = "RUN_CONFIGURATION_ID";

    @Nonnull
    @Override
    public String getId() {
        return "run_configuration";
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.actionsExecute();
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Run Configurations");
    }

    @Nonnull
    @Override
    public ConfigurationFileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
        return new RunConfigurationFileEditor(project, file);
    }
}
