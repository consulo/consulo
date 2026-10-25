// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal;

import consulo.diff.FrameDiffTool;
import consulo.diff.PrevNextDifferenceIterable;
import consulo.navigation.Navigatable;
import org.jspecify.annotations.Nullable;

public interface DiffViewerEx extends FrameDiffTool.DiffViewer {
    default @Nullable PrevNextDifferenceIterable getDifferenceIterable() {
        return null;
    }

    default @Nullable Navigatable getNavigatable() {
        return null;
    }
}
