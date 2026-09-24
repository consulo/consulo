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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import org.jspecify.annotations.Nullable;

/**
 * Builds the views of a services tool window - the swing ones need an awt hierarchy to be drawn, a unified frontend
 * gets views of its own. {@link ServiceViewManagerImpl} itself does not depend on either.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ServiceViewFactory {
    static ServiceViewFactory getInstance() {
        return Application.get().getInstance(ServiceViewFactory.class);
    }

    /**
     * A view of the model, with the state it was saved with.
     */
    @RequiredUIAccess
    BaseServiceView createView(Project project, ServiceViewModel viewModel, ServiceViewState viewState);

    /**
     * The content a view is shown in - the view can be found by the content again under {@link BaseServiceView#KEY}.
     */
    @RequiredUIAccess
    Content createContent(BaseServiceView serviceView, @Nullable String displayName, boolean isLockable);

    /**
     * What the tool window adds to the views - scrolling to and from the source, dropping items to extract them.
     */
    @RequiredUIAccess
    void installToolWindowSupport(Project project, ToolWindow toolWindow, ContentManager contentManager);
}
