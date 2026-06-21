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
package consulo.ide.impl.idea.build;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.build.ui.BuildContentManager;
import consulo.compiler.localize.CompilerLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 2026-03-21
 */
@ExtensionImpl
public class BuildToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public String getId() {
        return BuildContentManager.TOOL_WINDOW_ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();

        ContentManagerWatcher.watchContentManager(toolWindow, contentManager);
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return true;
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return false;
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsToolwindowbuild();
    }

    @Override
    public boolean canCloseContents() {
        return true;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return CompilerLocalize.toolwindowBuildDisplayName();
    }
}
