// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.timeline;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.ColorUtil;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

public final class StatusMessageComponentFactory {
    private StatusMessageComponentFactory() {
    }

    public static @Nonnull JComponent create(@Nonnull JComponent messageComponent) {
        return create(messageComponent, StatusMessageType.INFO);
    }

    public static @Nonnull JComponent create(@Nonnull JComponent messageComponent, @Nonnull StatusMessageType type) {
        VerticalRoundedLineComponent line = new VerticalRoundedLineComponent(6);
        line.setForeground(switch (type) {
            case INFO -> JBColor.namedColor("Review.MetaInfo.StatusLine.Blue", ColorUtil.fromHex("#40B6E0B2"));
            case SECONDARY_INFO -> JBColor.namedColor("Review.MetaInfo.StatusLine.Gray", ColorUtil.fromHex("#9AA7B0B3"));
            case SUCCESS -> JBColor.namedColor("Review.MetaInfo.StatusLine.Green", ColorUtil.fromHex("#62B543B3"));
            case WARNING, ERROR -> JBColor.namedColor("Review.MetaInfo.StatusLine.Orange", ColorUtil.fromHex("#F26522B3"));
        });
        messageComponent.setBorder(JBUI.Borders.empty(2, 0));
        return JBUI.Panels.simplePanel(8, 0)
            .addToCenter(messageComponent)
            .addToLeft(line)
            .andTransparent();
    }
}
