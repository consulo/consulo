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
import consulo.application.util.HtmlChunk;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.review.CodeReviewTimelineUIUtil;
import consulo.review.CodeReviewTitleUIUtil;
import consulo.ui.Button;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.ColorValueUtil;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.SimpleContentRevision;
import consulo.versionControlSystem.review.action.ReviewMergeCommitMessageDialog;
import consulo.versionControlSystem.ui.BranchPresentation;
import consulo.versionControlSystem.ui.ChangesGroupingSupport;
import consulo.versionControlSystem.ui.VcsChangesBrowser;
import consulo.versionControlSystem.ui.VcsChangesDialogFactory;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.inject.Inject;

import java.util.Date;
import java.util.List;
import java.util.Set;

@ExtensionImpl(id = "reviewPieces", order = "after createReview")
public class ReviewPiecesUITesterTab implements UITesterTab {
    private final ProjectManager myProjectManager;
    private final DialogService myDialogService;
    private final VcsChangesDialogFactory myVcsChangesDialogFactory;

    @Inject
    public ReviewPiecesUITesterTab(ProjectManager projectManager,
                                   DialogService dialogService,
                                   VcsChangesDialogFactory vcsChangesDialogFactory) {
        myProjectManager = projectManager;
        myDialogService = dialogService;
        myVcsChangesDialogFactory = vcsChangesDialogFactory;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Review Pieces");
    }

    @RequiredUIAccess
    @Override
    public consulo.ui.Component createComponent(Disposable uiDisposable) {
        Project project = myProjectManager.getDefaultProject();

        VerticalLayout top = VerticalLayout.create();
        top.add(HtmlLabel.create(LocalizeValue.of(
            CodeReviewTitleUIUtil.createTitleText("Port <b>combined</b> diff", "#42", "https://example.com/42", LocalizeValue.of("Open in browser")))));
        top.add(CodeReviewTimelineUIUtil.createTitleTextPane(LocalizeValue.of("Sand Reviewer"), "https://example.com/reviewer", new Date()));

        for (ToastStyle style : new ToastStyle[]{ToastStyle.INFO, null, ToastStyle.SUCCESS, ToastStyle.WARNING, ToastStyle.ERROR}) {
            Toast statusMessage = Toast.create();
            if (style != null) {
                statusMessage.addStyle(style);
            }
            statusMessage.addStyle(ToastStyle.LEFT_LINE);
            statusMessage.setText(LocalizeValue.of("Status message: " + (style == null ? "secondary" : style.name().toLowerCase())));
            top.add(statusMessage);
        }

        String branchStyle = "color: #" + ColorValueUtil.toHex(BranchPresentation.TEXT_COLOR) +
            "; background: #" + ColorValueUtil.toHex(BranchPresentation.getBranchPresentationBackground(ComponentColors.LAYOUT));
        top.add(HtmlLabel.create(LocalizeValue.of("Merged into " + HtmlChunk.span(branchStyle).addText(" feature/combined-diff "))));

        Label mergeResult = Label.create(LocalizeValue.of("Merge message: -"));
        UIAccess uiAccess = UIAccess.current();
        Button merge = Button.create(LocalizeValue.of("Merge…"), event -> {
            ReviewMergeCommitMessageDialog descriptor = new ReviewMergeCommitMessageDialog(project,
                LocalizeValue.of("Merge Review"), "Merge pull request #42", "Port combined diff");
            myDialogService.build(descriptor).showAsync().whenComplete((value, error) -> {
                if (value == DialogValue.OK_VALUE) {
                    uiAccess.give(() -> mergeResult.setText(LocalizeValue.of("Merge message: " + descriptor.getMessage())));
                }
            });
        });
        top.add(merge);
        top.add(mergeResult);

        Label groupingLabel = Label.create(LocalizeValue.of("Grouping: [" + ChangesGroupingSupport.MODULE_GROUPING + "]"));
        VcsChangesBrowser browser = myVcsChangesDialogFactory.changesBrowser(project)
            .grouping(Set.of(ChangesGroupingSupport.MODULE_GROUPING),
                keys -> groupingLabel.setText(LocalizeValue.of("Grouping: " + keys)))
            .build(uiDisposable);
        browser.setChanges(List.of(
            new Change(null, new SimpleContentRevision("a", VcsUtil.getFilePath("/sand/src/a/First.txt", false), "1")),
            new Change(null, new SimpleContentRevision("b", VcsUtil.getFilePath("/sand/src/b/Second.txt", false), "1"))
        ));
        top.add(groupingLabel);

        DockLayout root = DockLayout.create();
        root.top(top);
        root.center(browser.getComponent());
        return root;
    }
}
