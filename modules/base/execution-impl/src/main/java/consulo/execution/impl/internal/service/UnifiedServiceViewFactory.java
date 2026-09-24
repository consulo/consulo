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
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-24
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedServiceViewFactory implements ServiceViewFactory {
    @RequiredUIAccess
    @Override
    public BaseServiceView createView(Project project, ServiceViewModel viewModel, ServiceViewState viewState) {
        return UnifiedServiceView.createView(project, viewModel, viewState);
    }

    @RequiredUIAccess
    @Override
    public Content createContent(BaseServiceView serviceView, @Nullable String displayName, boolean isLockable) {
        Content content = ContentFactory.getInstance().createUIContent(((UnifiedServiceView) serviceView).getUIComponent(), displayName, isLockable);
        content.putUserData(BaseServiceView.KEY, serviceView);
        return content;
    }

    /**
     * Scrolling to the source and dropping items onto the tool window work on swing trees only.
     */
    @RequiredUIAccess
    @Override
    public void installToolWindowSupport(Project project, ToolWindow toolWindow, ContentManager contentManager) {
    }
}
