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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class ModuleGroupConfigurable implements MasterDetailsConfigurable<ModuleGroup> {
    private final ModuleGroup myModuleGroup;

    private @Nullable Component myComponent;

    public ModuleGroupConfigurable(ModuleGroup moduleGroup) {
        myModuleGroup = moduleGroup;
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        Component component = myComponent;
        if (component == null) {
            DockLayout panel = DockLayout.create();
            panel.top(HtmlLabel.create(ProjectLocalize.projectRootsModuleGroupsText()));
            component = panel;
            myComponent = component;
        }
        return component;
    }

    @Override
    public ModuleGroup getEditableObject() {
        return myModuleGroup;
    }

    @Override
    public LocalizeValue getBannerSlogan() {
        return ProjectLocalize.moduleGroupBannerText(myModuleGroup.toString());
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.of(myModuleGroup.toString());
    }

    @Override
    public @Nullable Image getIcon() {
        return PlatformIconGroup.nodesModulegroup();
    }

    @RequiredUIAccess
    @Override
    public void updateName() {
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myComponent = null;
    }
}
