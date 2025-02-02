/*
 * Copyright 2013-2021 consulo.io
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

import consulo.configuration.editor.ConfigurableFileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class RunConfigurationFileEditor extends ConfigurableFileEditor<RunConfigurable> {
    public RunConfigurationFileEditor(Project project, VirtualFile file) {
        super(project, file);
    }

    @Override
    public void onUpdateRequestParams(@Nonnull Map<String, String> params) {
        if (myConfigurable != null) {
            myProject.getUIAccess().give(() -> myConfigurable.selectFromManager());
        }
    }

    @Nonnull
    @Override
    protected RunConfigurable createConfigurable() {
        return new RunConfigurable(myProject);
    }

    @Override
    protected void onApply(RunConfigurable configurable) {
        configurable.updateActiveConfigurationFromSelected();
    }
}
