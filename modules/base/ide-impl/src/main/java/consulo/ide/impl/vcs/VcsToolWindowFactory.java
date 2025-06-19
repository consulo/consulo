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
package consulo.ide.impl.vcs;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentI;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.util.lang.Trinity;
import consulo.versionControlSystem.VcsToolWindow;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-10-30
 */
@ExtensionImpl
public class VcsToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Nonnull
    @Override
    public String getId() {
        return VcsToolWindow.ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        final ContentManager contentManager = toolWindow.getContentManager();

        ChangesViewContentManager manager = (ChangesViewContentManager) ChangesViewContentManager.getInstance(project);

        manager.loadExtensionTabs();

        Trinity<Image, LocalizeValue, Boolean> state = manager.getAlreadyLoadedState();
        manager.setAlreadyLoadedState(null);
        List<Content> contents = manager.setContentManager(toolWindow, contentManager);

        final List<Content> ordered = ChangesViewContentManager.doPresetOrdering(contents);
        for (Content content : ordered) {
            contentManager.addContent(content);
        }

        if (contentManager.getContentCount() > 0) {
            contentManager.setSelectedContent(contentManager.getContent(0));
        }

        if (state != null) {
            toolWindow.setIcon(state.getFirst());
            toolWindow.setDisplayName(state.getSecond());
            toolWindow.setAvailable(state.getThird(), null);
        }
    }

    @Override
    public boolean shouldBeAvailable(@Nonnull Project project) {
        ChangesViewContentManager manager = (ChangesViewContentManager) project.getInstanceIfCreated(ChangesViewContentI.class);
        if (manager != null) {
            Trinity<Image, LocalizeValue, Boolean> alreadyLoadedState = manager.getAlreadyLoadedState();
            return alreadyLoadedState != null && alreadyLoadedState.getThird();
        }
        return false;
    }

    @Nonnull
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsToolwindowchanges();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Version Control");
    }
}
