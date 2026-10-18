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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.review.details.CodeReviewDetailsComponentFactory;
import consulo.review.list.ReviewListComponentFactory;
import consulo.review.timeline.CodeReviewTimelineComponentFactory;
import consulo.review.toolwindow.ReviewToolwindowTabs;
import consulo.sandboxPlugin.ide.review.SandReviewProjectViewModel;
import consulo.sandboxPlugin.ide.review.SandReviewService;
import consulo.sandboxPlugin.ide.review.SandReviewTab;
import consulo.sandboxPlugin.ide.review.SandReviewTabViewModel;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import jakarta.inject.Inject;

/**
 * Previews the review components of the platform against the sand review service, without a tool window.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl(id = "review", order = "after messageList")
public class ReviewUITesterTab implements UITesterTab {
    private final ProjectManager myProjectManager;
    private final ReviewListComponentFactory myListComponentFactory;
    private final CodeReviewDetailsComponentFactory myDetailsComponentFactory;
    private final CodeReviewTimelineComponentFactory myTimelineComponentFactory;

    @Inject
    public ReviewUITesterTab(ProjectManager projectManager,
                             ReviewListComponentFactory listComponentFactory,
                             CodeReviewDetailsComponentFactory detailsComponentFactory,
                             CodeReviewTimelineComponentFactory timelineComponentFactory) {
        myProjectManager = projectManager;
        myListComponentFactory = listComponentFactory;
        myDetailsComponentFactory = detailsComponentFactory;
        myTimelineComponentFactory = timelineComponentFactory;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Code Review");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Project project = myProjectManager.getDefaultProject();
        SandReviewProjectViewModel projectVm = project.getInstance(SandReviewService.class).getToolwindowVm().getProjectVm().get();

        Component list = myListComponentFactory.create(uiDisposable, projectVm.getProjectName(), projectVm.getListVm(),
            review -> projectVm.getListVm().open(review));

        DockLayout details = DockLayout.create();
        Disposable[] detailsDisposable = {Disposable.newDisposable()};
        Disposer.register(uiDisposable, () -> Disposer.dispose(detailsDisposable[0]));

        UIAccess uiAccess = UIAccess.current();
        showDetails(details, projectVm.getTabs().get(), detailsDisposable);
        Runnable unsubscribe = projectVm.getTabs().addListener(tabs -> uiAccess.give(() -> showDetails(details, tabs, detailsDisposable)));
        Disposer.register(uiDisposable, unsubscribe::run);

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(list);
        split.setSecondComponent(details);
        split.setProportion(35);
        return split;
    }

    @RequiredUIAccess
    private void showDetails(DockLayout details,
                             ReviewToolwindowTabs<SandReviewTab, SandReviewTabViewModel> tabs,
                             Disposable[] detailsDisposable) {
        Disposer.dispose(detailsDisposable[0]);
        detailsDisposable[0] = Disposable.newDisposable();

        SandReviewTab selected = tabs.selectedTab();
        SandReviewTabViewModel tabVm = selected == null ? null : tabs.tabs().get(selected);
        if (tabVm == null) {
            details.center(Label.create(LocalizeValue.localizeTODO("Select a review")));
            return;
        }

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        split.setFirstComponent(myDetailsComponentFactory.create(detailsDisposable[0], tabVm, null));
        split.setSecondComponent(myTimelineComponentFactory.create(detailsDisposable[0], tabVm, tabVm.getTimelineVm()));
        split.setProportion(40);
        details.center(split);
    }
}
