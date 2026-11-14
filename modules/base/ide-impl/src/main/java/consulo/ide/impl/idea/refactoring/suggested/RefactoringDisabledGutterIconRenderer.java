// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class RefactoringDisabledGutterIconRenderer extends GutterIconRenderer {
    private final LocalizeValue myTooltip;

    public RefactoringDisabledGutterIconRenderer(LocalizeValue tooltip) {
        myTooltip = tooltip;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.gutterSuggestedrefactoringbulbdisabled();
    }

    @Override
    public LocalizeValue getTooltipValue() {
        return myTooltip;
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.RIGHT;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
