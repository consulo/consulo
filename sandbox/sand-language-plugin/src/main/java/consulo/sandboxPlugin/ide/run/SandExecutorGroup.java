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

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.executor.DefaultExecutorGroup;
import consulo.localize.LocalizeValue;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public class SandExecutorGroup extends DefaultExecutorGroup<SandExecutorSettings> {
    public static final String EXECUTOR_ID = "SandProfileGroup";

    public SandExecutorGroup() {
        registerSettings(new SandExecutorSettings(
            LocalizeValue.localizeTODO("Fast"),
            LocalizeValue.localizeTODO("Profile (Fast)"),
            LocalizeValue.localizeTODO("Profile with Fast Sampling"),
            PlatformIconGroup.actionsProfilecpu()
        ));
        registerSettings(new SandExecutorSettings(
            LocalizeValue.localizeTODO("Detailed"),
            LocalizeValue.localizeTODO("Profile (Detailed)"),
            LocalizeValue.localizeTODO("Profile with Detailed Tracing"),
            PlatformIconGroup.actionsProfilememory()
        ));
    }

    @Override
    public String getId() {
        return EXECUTOR_ID;
    }

    @Override
    public String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    public Image getToolWindowIcon() {
        return PlatformIconGroup.toolwindowsToolwindowrun();
    }

    @Override
    public Image getToolWindowIconIfRunning() {
        return PlatformIconGroup.toolwindowsToolwindowrunactive();
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.actionsProfile();
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Profile the selected sand configuration");
    }

    @Override
    public LocalizeValue getActionName() {
        return LocalizeValue.localizeTODO("Sand Profile");
    }

    @Override
    public LocalizeValue getStartActionText() {
        return LocalizeValue.localizeTODO("Profile");
    }

    @Override
    public LocalizeValue getStartActiveText(String configurationName) {
        return LocalizeValue.localizeTODO("Profile '" + configurationName + "'");
    }

    @Override
    public LocalizeValue getRunToolbarActionText(String param) {
        return LocalizeValue.localizeTODO("Profile: " + param);
    }

    @Override
    public LocalizeValue getRunToolbarChooserText() {
        return LocalizeValue.localizeTODO("Profile");
    }

    @Override
    public boolean isApplicable(Project project) {
        return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SandModuleExtension.class);
    }
}
