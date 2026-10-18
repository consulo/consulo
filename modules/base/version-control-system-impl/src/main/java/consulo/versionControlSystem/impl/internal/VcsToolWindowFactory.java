/*
 * Copyright 2013-2020 consulo.io
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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.versionControlSystem.impl.internal.change.ui.UnifiedChangesViewContentManager;
import consulo.versionControlSystem.internal.ChangesViewContentI;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.versionControlSystem.VcsToolWindow;


/**
 * @author VISTALL
 * @since 2020-10-30
 */
@ExtensionImpl
public class VcsToolWindowFactory implements ToolWindowFactory, DumbAware {
    
    @Override
    public String getId() {
        return VcsToolWindow.ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        UnifiedChangesViewContentManager.getInstanceImpl(project).attachToolWindow(toolWindow);
    }

    @Override
    public boolean canCloseContents() {
        return true;
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        if (project.getInstanceIfCreated(ChangesViewContentI.class) instanceof UnifiedChangesViewContentManager manager) {
            return manager.isToolWindowAvailable();
        }
        return false;
    }

    
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    
    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsToolwindowchanges();
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Version Control");
    }
}
