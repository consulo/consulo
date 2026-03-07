// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import consulo.diff.util.LineRange;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nullable;

/**
 * A UI model for an editor with gutter changes highlighting and a popup with actions for changes
 * This model should exist in the same scope as the editor
 * One model - one editor
 */
public interface CodeReviewEditorGutterActionableChangesModel extends CodeReviewEditorGutterChangesModel {
    @RequiresEdt
    boolean getShouldHighlightDiffRanges();

    @RequiresEdt
    void setShouldHighlightDiffRanges(boolean value);

    @RequiresEdt
    @Nullable
    String getBaseContent(LineRange lines);

    @RequiresEdt
    void showDiff(@Nullable Integer lineIdx);

    @RequiresEdt
    void addDiffHighlightListener(Disposable disposable, Runnable listener);

    Key<CodeReviewEditorGutterActionableChangesModel> KEY =
        Key.create(CodeReviewEditorGutterActionableChangesModel.class.getCanonicalName());
}
