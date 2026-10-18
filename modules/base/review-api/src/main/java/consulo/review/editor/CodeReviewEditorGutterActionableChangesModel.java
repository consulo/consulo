// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * A UI model for an editor with gutter changes highlighting and a popup with actions for changes
 * This model should exist in the same scope as the editor
 * One model - one editor
 */
public interface CodeReviewEditorGutterActionableChangesModel extends CodeReviewEditorGutterChangesModel {
    Key<CodeReviewEditorGutterActionableChangesModel> KEY = Key.create(CodeReviewEditorGutterActionableChangesModel.class.getCanonicalName());

    @RequiredUIAccess
    boolean getShouldHighlightDiffRanges();

    @RequiredUIAccess
    void setShouldHighlightDiffRanges(boolean value);

    /**
     * @param baseLine1 the first line of the review base, inclusive
     * @param baseLine2 the last line of the review base, exclusive
     */
    @RequiredUIAccess
    @Nullable String getBaseContent(int baseLine1, int baseLine2);

    @RequiredUIAccess
    void showDiff(@Nullable Integer lineIdx);

    @RequiredUIAccess
    void addDiffHighlightListener(Disposable disposable, Runnable listener);
}
