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
package consulo.review.toolwindow;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;

/**
 * Manages review toolwindow tabs and their content.
 * If {@link ReviewToolwindowViewModel#getProjectVm()} in {@code reviewToolwindowViewModel} is null UI for acquiring this context is
 * shown (by {@link ReviewTabsComponentFactory#createEmptyTabContent}).
 * <p>
 * When {@link ReviewToolwindowViewModel#getProjectVm()} appears Review List will be shown
 * (by {@link ReviewTabsComponentFactory#createReviewListComponent}), and {@link ReviewToolwindowProjectViewModel#getTabs()} will be
 * managed, according to described in {@link ReviewTab} {@link ReviewTab#getId()} logic.
 * So, new tabs will be shown using {@link ReviewTabsComponentFactory#createTabComponent}.
 * <p>
 * Several review view models may share one tool window, and each only touches the contents created for it.
 *
 * @author VISTALL
 * @see ReviewToolwindowDataKeys
 * @since 2026-10-18
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ReviewToolwindowTabsManager {
    /**
     * @param parentDisposable the contents of {@code reviewToolwindowViewModel} are removed when it is disposed
     * @param tabTitle         the title of a content before it gets its own display name
     */
    @RequiredUIAccess
    <T extends ReviewTab, TVM extends ReviewTabViewModel, PVM extends ReviewToolwindowProjectViewModel<T, TVM>> void manageReviewToolwindowTabs(
        Disposable parentDisposable,
        Project project,
        ToolWindow toolwindow,
        ReviewToolwindowViewModel<PVM> reviewToolwindowViewModel,
        ReviewTabsComponentFactory<TVM, PVM> tabComponentFactory,
        LocalizeValue tabTitle
    );
}
