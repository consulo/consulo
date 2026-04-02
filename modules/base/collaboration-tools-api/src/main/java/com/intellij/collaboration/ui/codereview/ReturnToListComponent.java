// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

import consulo.ui.ex.awt.*;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @deprecated Deprecated with the move to a different design
 */
@Deprecated
public final class ReturnToListComponent {
    private ReturnToListComponent() {
    }

    public static @Nonnull JComponent createReturnToListSideComponent(
        @Nls @Nonnull String text,
        @Nonnull Runnable onClick
    ) {
        LinkLabel<Object> link = new LinkLabel<>(UIUtil.leftArrow() + " " + text, null, (aSource, aLinkData) -> onClick.run());
        link.setBorder(JBUI.Borders.emptyRight(8));
        return new BorderLayoutPanel()
            .addToRight(link)
            .andTransparent()
            .withBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
    }
}
