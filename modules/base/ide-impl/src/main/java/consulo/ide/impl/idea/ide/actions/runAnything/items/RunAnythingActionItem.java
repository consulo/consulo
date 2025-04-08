// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.items;

import consulo.ide.runAnything.RunAnythingItemBase;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RunAnythingActionItem<T extends AnAction> extends RunAnythingItemBase {
    @Nonnull
    private final T myAction;

    public RunAnythingActionItem(@Nonnull T action, @Nonnull String fullCommand, @Nullable Image icon) {
        super(fullCommand, icon);
        myAction = action;
    }

    @Nonnull
    public static String getCommand(@Nonnull AnAction action, @Nonnull String command) {
        return command + " " + (
            action.getTemplatePresentation().getText() != null
                ? action.getTemplatePresentation().getText()
                : "undefined"
        );
    }

    @Nullable
    @Override
    public String getDescription() {
        return myAction.getTemplatePresentation().getDescription();
    }
}