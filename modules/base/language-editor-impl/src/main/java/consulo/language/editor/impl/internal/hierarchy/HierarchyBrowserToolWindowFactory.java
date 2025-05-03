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
package consulo.language.editor.impl.internal.hierarchy;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-05-03
 */
@ExtensionImpl
public class HierarchyBrowserToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Nonnull
    @Override
    public String getId() {
        return ToolWindowId.HIERARCHY;
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return true;
    }

    @Override
    public boolean shouldBeAvailable(@Nonnull Project project) {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        ContentManagerWatcher.watchContentManager(toolWindow, toolWindow.getContentManager());
    }

    @Override
    public boolean canCloseContents() {
        return true;
    }

    @Nonnull
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsToolwindowhierarchy();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return UILocalize.toolWindowNameHierarchy();
    }
}
