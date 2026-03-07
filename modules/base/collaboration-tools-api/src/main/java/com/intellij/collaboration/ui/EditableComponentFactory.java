// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import com.intellij.collaboration.ui.codereview.comment.CodeReviewCommentTextFieldFactory;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewTextEditingViewModel;
import com.intellij.collaboration.ui.codereview.comment.EditActionsConfigKt;
import com.intellij.collaboration.ui.util.SwingBindingsUtil;
import com.intellij.ui.components.panels.Wrapper;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.function.BiFunction;

public final class EditableComponentFactory {
    private EditableComponentFactory() {
    }

    public static <VM> @Nonnull JComponent create(
        @Nonnull CoroutineScope cs,
        @Nonnull JComponent component,
        @Nonnull Flow<VM> editingVm,
        @Nonnull BiFunction<CoroutineScope, VM, JComponent> editorComponentSupplier
    ) {
        Wrapper wrapper = new Wrapper();
        SwingBindingsUtil.bindContentIn(wrapper, cs, editingVm, (scope, vm) -> {
            if (vm != null) {
                return editorComponentSupplier.apply(scope, vm);
            }
            return component;
        });
        return wrapper;
    }

    public static @Nonnull JComponent wrapTextComponent(
        @Nonnull CoroutineScope cs,
        @Nonnull JComponent component,
        @Nonnull Flow<CodeReviewTextEditingViewModel> editVmFlow
    ) {
        return wrapTextComponent(cs, component, editVmFlow, () -> {
        });
    }

    public static @Nonnull JComponent wrapTextComponent(
        @Nonnull CoroutineScope cs,
        @Nonnull JComponent component,
        @Nonnull Flow<CodeReviewTextEditingViewModel> editVmFlow,
        @Nonnull Runnable afterSave
    ) {
        return create(cs, component, editVmFlow, (scope, editVm) -> {
            var actions = EditActionsConfigKt.createEditActionsConfig(editVm, afterSave);
            return CodeReviewCommentTextFieldFactory.createIn(scope, editVm, actions);
        });
    }
}
