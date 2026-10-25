// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import consulo.codeEditor.Editor;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Shows the review in one editor of a diff viewer while {@code scope} lives.
 */
@FunctionalInterface
public interface EditorCodeReviewRenderer {
    /**
     * @param side   the side the editor shows, {@code null} for the editor showing both sides
     * @param mapper the lines of the editor against the lines of the sides
     */
    @RequiredUIAccess
    void render(Disposable scope, Editor editor, @Nullable Side side, DiffEditorLineMapper mapper);
}
