// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.diff;

import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.keymap.KeymapUtil;
import consulo.application.AllIcons;
import consulo.application.util.HtmlChunk;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import jakarta.annotation.Nonnull;

import javax.swing.*;

@Deprecated
public abstract class AddCommentGutterIconRenderer extends LineGutterIconRenderer {
    private @Nonnull Icon visibleIcon = AllIcons.General.InlineAdd;

    @Override
    public @Nonnull Icon getVisibleIcon() {
        return visibleIcon;
    }

    @Override
    public void setVisibleIcon(@Nonnull Icon icon) {
        this.visibleIcon = icon;
    }

    @Override
    public String getTooltipText() {
        return HtmlChunk.html()
            .addText(CollaborationToolsLocalize.diffAddCommentIconTooltip())
            .addRaw(HelpTooltip.getShortcutAsHtml(KeymapUtil.getFirstKeyboardShortcutText(getShortcut())))
            .toString();
    }
}
