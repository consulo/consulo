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
package consulo.sandboxPlugin.ide.review;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.review.details.CodeReviewDetailsActionsComponentFactory;
import consulo.review.details.CodeReviewDetailsComponentFactory;
import consulo.review.details.CodeReviewDetailsStatusComponentFactory;
import consulo.review.icon.ReviewIconGroup;
import consulo.review.list.ReviewListComponentFactory;
import consulo.review.timeline.CodeReviewTimelineComponentFactory;
import consulo.review.toolwindow.ReviewToolwindowTabsManager;
import consulo.review.toolwindow.ToolwindowUtils;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.versionControlSystem.review.changes.CodeReviewChangeListComponentFactory;
import consulo.versionControlSystem.review.details.CodeReviewDetailsBranchComponentFactory;
import consulo.versionControlSystem.review.details.CodeReviewDetailsCommitInfoComponentFactory;
import consulo.versionControlSystem.review.details.CodeReviewDetailsCommitsComponentFactory;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class SandReviewToolWindowFactory implements ToolWindowFactory, DumbAware {
    private final ReviewToolwindowTabsManager myTabsManager;
    private final ReviewListComponentFactory myListComponentFactory;
    private final CodeReviewDetailsComponentFactory myDetailsComponentFactory;
    private final CodeReviewTimelineComponentFactory myTimelineComponentFactory;
    private final CodeReviewDetailsCommitsComponentFactory myCommitsComponentFactory;
    private final CodeReviewDetailsCommitInfoComponentFactory myCommitInfoComponentFactory;
    private final CodeReviewDetailsBranchComponentFactory myBranchComponentFactory;
    private final CodeReviewChangeListComponentFactory myChangeListComponentFactory;
    private final CodeReviewDetailsStatusComponentFactory myStatusComponentFactory;
    private final CodeReviewDetailsActionsComponentFactory myActionsComponentFactory;

    @Inject
    public SandReviewToolWindowFactory(ReviewToolwindowTabsManager tabsManager,
                                       ReviewListComponentFactory listComponentFactory,
                                       CodeReviewDetailsComponentFactory detailsComponentFactory,
                                       CodeReviewTimelineComponentFactory timelineComponentFactory,
                                       CodeReviewDetailsCommitsComponentFactory commitsComponentFactory,
                                       CodeReviewDetailsCommitInfoComponentFactory commitInfoComponentFactory,
                                       CodeReviewDetailsBranchComponentFactory branchComponentFactory,
                                       CodeReviewChangeListComponentFactory changeListComponentFactory,
                                       CodeReviewDetailsStatusComponentFactory statusComponentFactory,
                                       CodeReviewDetailsActionsComponentFactory actionsComponentFactory) {
        myTabsManager = tabsManager;
        myStatusComponentFactory = statusComponentFactory;
        myActionsComponentFactory = actionsComponentFactory;
        myCommitsComponentFactory = commitsComponentFactory;
        myCommitInfoComponentFactory = commitInfoComponentFactory;
        myBranchComponentFactory = branchComponentFactory;
        myChangeListComponentFactory = changeListComponentFactory;
        myListComponentFactory = listComponentFactory;
        myDetailsComponentFactory = detailsComponentFactory;
        myTimelineComponentFactory = timelineComponentFactory;
    }

    @Override
    public String getId() {
        return "Sand Review";
    }

    @Override
    public boolean canCloseContents() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ToolwindowUtils.dontHideOnEmptyContent(toolWindow);
        myTabsManager.manageReviewToolwindowTabs(
            toolWindow.getContentManager(),
            project,
            toolWindow,
            project.getInstance(SandReviewService.class).getToolwindowVm(),
            new SandReviewTabsComponentFactory(project,
                myListComponentFactory,
                myDetailsComponentFactory,
                myTimelineComponentFactory,
                myCommitsComponentFactory,
                myCommitInfoComponentFactory,
                myBranchComponentFactory,
                myChangeListComponentFactory,
                myStatusComponentFactory,
                myActionsComponentFactory),
            LocalizeValue.localizeTODO("Sand Review")
        );
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.LEFT;
    }

    @Override
    public Image getIcon() {
        return ReviewIconGroup.pullrequestopen();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Sand Review");
    }
}
