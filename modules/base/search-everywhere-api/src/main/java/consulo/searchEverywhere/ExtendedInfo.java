// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.searchEverywhere;

import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Extended information shown in the footer of the Search Everywhere popup for a selected item.
 */
public class ExtendedInfo {
    private Function<Object, @Nullable String> leftText;
    private Function<Object, @Nullable AnAction> rightAction;

    public ExtendedInfo() {
        this.leftText = o -> null;
        this.rightAction = o -> null;
    }

    public ExtendedInfo(Function<Object, @Nullable String> leftText, Function<Object, @Nullable AnAction> rightAction) {
        this.leftText = leftText;
        this.rightAction = rightAction;
    }

    public Function<Object, @Nullable String> getLeftText() {
        return leftText;
    }

    public void setLeftText(Function<Object, @Nullable String> leftText) {
        this.leftText = leftText;
    }

    public Function<Object, @Nullable AnAction> getRightAction() {
        return rightAction;
    }

    public void setRightAction(Function<Object, @Nullable AnAction> rightAction) {
        this.rightAction = rightAction;
    }
}
