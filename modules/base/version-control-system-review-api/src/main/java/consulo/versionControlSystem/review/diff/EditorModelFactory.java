// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import consulo.codeEditor.Editor;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @param <M> editor inlays and controls model
 */
@FunctionalInterface
public interface EditorModelFactory<M> {
    @RequiredUIAccess
    M create(Disposable scope, Editor editor, @Nullable Side side, DiffEditorLineMapper mapper);
}
