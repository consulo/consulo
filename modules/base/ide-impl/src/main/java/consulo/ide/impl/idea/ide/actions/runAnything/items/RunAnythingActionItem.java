// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.items;

import consulo.ide.runAnything.RunAnythingItemBase;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class RunAnythingActionItem<T extends AnAction> extends RunAnythingItemBase {
    
    private final T myAction;

    public RunAnythingActionItem(T action, String fullCommand, @Nullable Image icon) {
        super(fullCommand, icon);
        myAction = action;
    }

    
    public static String getCommand(AnAction action, String command) {
        return command + " " + (
            action.getTemplatePresentation().getText() != null
                ? action.getTemplatePresentation().getText()
                : "undefined"
        );
    }

    @Override
    public @Nullable String getDescription() {
        return myAction.getTemplatePresentation().getDescription();
    }
}