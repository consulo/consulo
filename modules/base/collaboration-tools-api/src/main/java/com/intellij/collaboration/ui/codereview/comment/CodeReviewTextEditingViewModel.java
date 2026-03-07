// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.ui.util.SwingActionUtilKt;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.platform.base.localize.CommonLocalize;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

import java.util.List;

public interface CodeReviewTextEditingViewModel extends CodeReviewSubmittableTextViewModel {
    /**
     * Submit the new text in background
     */
    void save();

    void stopEditing();

    static @Nonnull CommentInputActionsComponentFactory.Config createEditActionsConfig(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewTextEditingViewModel editVm
    ) {
        return createEditActionsConfig(cs, editVm, () -> {
        });
    }

    static @Nonnull CommentInputActionsComponentFactory.Config createEditActionsConfig(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewTextEditingViewModel editVm,
        @Nonnull Runnable afterSave
    ) {
      return new CommentInputActionsComponentFactory.Config(
            StateFlowKt.MutableStateFlow(CodeReviewCommentTextFieldFactory.submitActionIn(
                cs,
                editVm,
                CollaborationToolsLocalize.reviewCommentSave().get(),
                vm -> {
                    vm.save();
                    afterSave.run();
                }
            )),
            StateFlowKt.MutableStateFlow(List.of()),
            StateFlowKt.MutableStateFlow(List.of()),
            StateFlowKt.MutableStateFlow(SwingActionUtilKt.swingAction(CommonLocalize.buttonCancel().get(), e -> editVm.stopEditing())),
            StateFlowKt.MutableStateFlow(
                CollaborationToolsLocalize.reviewCommentSaveHint(CommentInputActionsComponentFactory.getSubmitShortcutText()).get()
            )
        );
    }
}
