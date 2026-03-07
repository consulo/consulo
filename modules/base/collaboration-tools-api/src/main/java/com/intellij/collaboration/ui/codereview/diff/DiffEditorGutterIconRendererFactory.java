// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.diff;

import jakarta.annotation.Nonnull;

@Deprecated
public interface DiffEditorGutterIconRendererFactory {
    @Nonnull
    AddCommentGutterIconRenderer createCommentRenderer(int line);
}
