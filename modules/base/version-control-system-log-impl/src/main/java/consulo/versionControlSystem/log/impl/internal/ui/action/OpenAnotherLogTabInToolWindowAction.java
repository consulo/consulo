/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.versionControlSystem.internal.ChangesViewContentI;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.impl.internal.VcsLogContentProvider;
import consulo.versionControlSystem.log.impl.internal.VcsLogFilterCollectionImpl;
import consulo.versionControlSystem.log.impl.internal.VcsLogTabLocation;
import consulo.versionControlSystem.log.impl.internal.VcsProjectLogImpl;
import org.jspecify.annotations.Nullable;

@ActionImpl(id = "Vcs.Log.OpenAnotherTab")
public class OpenAnotherLogTabInToolWindowAction extends OpenAnotherLogTabAction {
    public OpenAnotherLogTabInToolWindowAction() {
        super(PlatformIconGroup.generalAdd());
    }

    @Override
    protected VcsLogTabLocation getLocation(AnActionEvent e) {
        return VcsLogTabLocation.TOOL_WINDOW;
    }

    @Override
    protected VcsLogFilterCollection getFilters(Project project, AnActionEvent e) {
        VcsLogUi logUi = findSelectedLogUi(project);
        return logUi == null ? VcsLogFilterCollectionImpl.EMPTY : logUi.getFilterUi().getFilters();
    }

    private static @Nullable VcsLogUi findSelectedLogUi(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);
        Content content = toolWindow == null ? null : toolWindow.getContentManager().getSelectedContent();
        if (content == null) {
            return null;
        }
        if (content.getDisposer() instanceof VcsLogUi logUi) {
            return logUi;
        }
        if (VcsLogContentProvider.TAB_NAME.equals(content.getUserData(ChangesViewContentI.CONTENT_ID_KEY))) {
            return VcsProjectLogImpl.getInstance(project).getMainLogUi();
        }
        return null;
    }
}
