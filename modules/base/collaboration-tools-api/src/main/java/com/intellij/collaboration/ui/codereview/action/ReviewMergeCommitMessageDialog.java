// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.action;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.CommitMessage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public final class ReviewMergeCommitMessageDialog extends DialogWrapper {
    private final CommitMessage commitMessage;

    public ReviewMergeCommitMessageDialog(
        @Nonnull Project project,
        @NlsContexts.DialogTitle @Nonnull String title,
        @Nonnull String subject,
        @Nonnull String body
    ) {
        super(project);

        commitMessage = new CommitMessage(project, false, false, true);
        commitMessage.setCommitMessage(subject + "\n\n" + body);
        commitMessage.setPreferredSize(new JBDimension(500, 85));

        Disposer.register(getDisposable(), commitMessage);

        setTitle(title);
        setOKButtonText(CollaborationToolsLocalize.dialogReviewMergeCommitButtonMerge());
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return JBUI.Panels.simplePanel(0, UIUtil.DEFAULT_VGAP)
            .addToTop(new JLabel(CollaborationToolsLocalize.dialogReviewMergeCommitMessage().get()))
            .addToCenter(commitMessage);
    }

    public @Nonnull String getMessage() {
        return commitMessage.getComment();
    }
}
