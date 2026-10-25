// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.action;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.CommitMessageComponent;
import consulo.versionControlSystem.CommitMessageFactory;

/**
 * Asks for the message of the commit merging a review. Show it with {@code DialogService}; {@link #getMessage()} holds the
 * message once the dialog closes with the merge button.
 */
public class ReviewMergeCommitMessageDialog extends DialogDescriptor {
    private final CommitMessageComponent myCommitMessage;

    @RequiredUIAccess
    public ReviewMergeCommitMessageDialog(Project project, LocalizeValue title, String subject, String body) {
        super(title);
        myCommitMessage = project.getInstance(CommitMessageFactory.class).create(false);
        myCommitMessage.setCommitMessage(subject + "\n\n" + body);
    }

    @Override
    public WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(540, 220);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        DockLayout layout = DockLayout.create();
        layout.top(Label.create(ReviewLocalize.reviewMergeCommitDialogMessage()));
        layout.center(myCommitMessage.getComponent());
        return layout;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(ReviewLocalize.reviewMergeCommitDialogButtonMerge());
    }

    public String getMessage() {
        return myCommitMessage.getCommitMessage();
    }
}
