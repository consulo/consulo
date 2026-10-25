// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.ui.annotation.RequiredUIAccess;

public interface CombinedDiffCaretNavigation {
    @RequiredUIAccess
    void moveCaretToNextBlock();

    @RequiredUIAccess
    void moveCaretToPrevBlock();

    @RequiredUIAccess
    void moveCaretPageUp();

    @RequiredUIAccess
    void moveCaretPageDown();
}
