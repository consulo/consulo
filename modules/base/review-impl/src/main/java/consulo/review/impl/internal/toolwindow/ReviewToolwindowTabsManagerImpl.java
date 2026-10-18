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
package consulo.review.impl.internal.toolwindow;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.review.toolwindow.ReviewTab;
import consulo.review.toolwindow.ReviewTabViewModel;
import consulo.review.toolwindow.ReviewTabsComponentFactory;
import consulo.review.toolwindow.ReviewToolwindowProjectViewModel;
import consulo.review.toolwindow.ReviewToolwindowTabsManager;
import consulo.review.toolwindow.ReviewToolwindowViewModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@Singleton
@ServiceImpl
public class ReviewToolwindowTabsManagerImpl implements ReviewToolwindowTabsManager {
    private final ContentFactory myContentFactory;

    @Inject
    public ReviewToolwindowTabsManagerImpl(ContentFactory contentFactory) {
        myContentFactory = contentFactory;
    }

    @RequiredUIAccess
    @Override
    public <T extends ReviewTab, TVM extends ReviewTabViewModel, PVM extends ReviewToolwindowProjectViewModel<T, TVM>> void manageReviewToolwindowTabs(
        Disposable parentDisposable,
        Project project,
        ToolWindow toolwindow,
        ReviewToolwindowViewModel<PVM> reviewToolwindowViewModel,
        ReviewTabsComponentFactory<TVM, PVM> tabComponentFactory,
        LocalizeValue tabTitle
    ) {
        new ReviewToolwindowTabsController<>(parentDisposable, project, toolwindow, myContentFactory, reviewToolwindowViewModel,
            tabComponentFactory, tabTitle);
    }
}
