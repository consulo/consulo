/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.content.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Belyaev
 */
@Singleton
@ServiceImpl
public class MessageViewImpl implements MessageView {
    private ToolWindow myToolWindow;
    private final List<Runnable> myPostponedRunnables = new ArrayList<Runnable>();

    @Inject
    public MessageViewImpl(Project project, StartupManager startupManager, ToolWindowManager toolWindowManager) {
        @RequiredUIAccess
        Runnable runnable = () -> {
            myToolWindow = toolWindowManager.registerToolWindow(ToolWindowId.MESSAGES_WINDOW, true, ToolWindowAnchor.BOTTOM, project, true);
            myToolWindow.setDisplayName(ProjectUILocalize.toolwindowMessagesDisplayName());
            myToolWindow.setIcon(PlatformIconGroup.toolwindowsToolwindowmessages());
            ContentManagerWatcher.watchContentManager(myToolWindow, getContentManager());
            for (Runnable postponedRunnable : myPostponedRunnables) {
                postponedRunnable.run();
            }
            myPostponedRunnables.clear();
        };
        if (project.isInitialized()) {
            runnable.run();
        }
        else {
            startupManager.registerPostStartupActivity(runnable::run);
        }
    }

    @Nonnull
    @Override
    public ToolWindow getToolWindow() {
        return myToolWindow;
    }

    @Nonnull
    @Override
    public ContentManager getContentManager() {
        return myToolWindow.getContentManager();
    }

    @Override
    public void runWhenInitialized(@Nonnull Runnable runnable) {
        if (myToolWindow != null) {
            runnable.run();
        }
        else {
            myPostponedRunnables.add(runnable);
        }
    }
}
