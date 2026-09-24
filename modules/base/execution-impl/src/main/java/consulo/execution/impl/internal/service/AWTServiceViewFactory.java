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
package consulo.execution.impl.internal.service;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-24
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class AWTServiceViewFactory implements ServiceViewFactory {
    /**
     * One handler for all the views of a project, as the manager kept it.
     */
    private static final Key<AutoScrollToSourceHandler> AUTO_SCROLL_TO_SOURCE_HANDLER = Key.create("services.auto.scroll.to.source.handler");

    @RequiredUIAccess
    @Override
    public BaseServiceView createView(Project project, ServiceViewModel viewModel, ServiceViewState viewState) {
        ServiceView serviceView = ServiceView.createView(project, viewModel, viewState);
        serviceView.setAutoScrollToSourceHandler(getAutoScrollToSourceHandler(project));
        return serviceView;
    }

    @RequiredUIAccess
    @Override
    public Content createContent(BaseServiceView serviceView, @Nullable String displayName, boolean isLockable) {
        Content content = ContentFactory.getInstance().createContent((ServiceView) serviceView, displayName, isLockable);
        content.putUserData(BaseServiceView.KEY, serviceView);
        return content;
    }

    @RequiredUIAccess
    @Override
    public void installToolWindowSupport(Project project, ToolWindow toolWindow, ContentManager contentManager) {
        ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;
        ServiceViewSourceScrollHelper.installAutoScrollSupport(project, toolWindowEx, getAutoScrollToSourceHandler(project));
        ServiceViewDragHelper.installDnDSupport(project, toolWindowEx.getDecorator(), contentManager);
    }

    private static AutoScrollToSourceHandler getAutoScrollToSourceHandler(Project project) {
        AutoScrollToSourceHandler handler = project.getUserData(AUTO_SCROLL_TO_SOURCE_HANDLER);
        if (handler == null) {
            handler = ServiceViewSourceScrollHelper.createAutoScrollToSourceHandler(project);
            project.putUserData(AUTO_SCROLL_TO_SOURCE_HANDLER, handler);
        }
        return handler;
    }
}
