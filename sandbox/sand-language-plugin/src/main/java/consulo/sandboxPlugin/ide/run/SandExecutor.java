/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.execution.executor.Executor;
import consulo.localize.LocalizeValue;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
@ExtensionImpl
public class SandExecutor extends Executor {
    @Override
    public String getToolWindowId() {
        return "SandExecutor";
    }

    @Override
    public Image getToolWindowIcon() {
        return AllIcons.Toolwindows.ToolWindowInspection;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return AllIcons.Ide.HectorOn;
    }

    @Override
    public Image getDisabledIcon() {
        return AllIcons.Ide.HectorOff;
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Sand executor");
    }

    @Override
    public boolean isApplicable(@Nonnull Project project) {
        return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SandModuleExtension.class);
    }

    @Nonnull
    @Override
    public LocalizeValue getActionName() {
        return LocalizeValue.localizeTODO("Sand");
    }

    @Nonnull
    @Override
    public String getId() {
        return "SandExecutor";
    }

    @Nonnull
    @Override
    public LocalizeValue getStartActionText() {
        return LocalizeValue.localizeTODO("Start Sand");
    }

    @Nonnull
    @Override
    public LocalizeValue getStartActiveText(@Nonnull String configurationName) {
        return LocalizeValue.localizeTODO("Start Sand " + configurationName);
    }
}
