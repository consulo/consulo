/*
 * Copyright 2013-2026 consulo.io
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

import consulo.execution.configuration.RunProfile;
import consulo.execution.executor.RunExecutorSettings;
import consulo.localize.LocalizeValue;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class SandExecutorSettings implements RunExecutorSettings {
    private final LocalizeValue myModeName;
    private final LocalizeValue myActionName;
    private final LocalizeValue myStartActionText;
    private final Image myIcon;

    public SandExecutorSettings(LocalizeValue modeName, LocalizeValue actionName, LocalizeValue startActionText, Image icon) {
        myModeName = modeName;
        myActionName = actionName;
        myStartActionText = startActionText;
        myIcon = icon;
    }

    public LocalizeValue getModeName() {
        return myModeName;
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    @Override
    public LocalizeValue getActionName() {
        return myActionName;
    }

    @Override
    public LocalizeValue getStartActionText() {
        return myStartActionText;
    }

    @Override
    public boolean isApplicable(Project project) {
        return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SandModuleExtension.class);
    }

    @Override
    public boolean canRun(RunProfile profile) {
        return profile instanceof SandConfiguration;
    }
}
