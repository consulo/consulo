// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public final class LfeEditorActionSearchBackHandler extends LfeEditorActionSearchAgainHandler {

    @Override
    protected boolean isForwardDirection() {
        return false;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_FIND_PREVIOUS;
    }
}
