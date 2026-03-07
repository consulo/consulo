// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;
import javax.swing.JPanel;

// 52 for avatar and gaps
public class CodeReviewComponentInlayRenderer extends ComponentInlayRenderer<JComponent> {
    private final JComponent actualComponent;

    public CodeReviewComponentInlayRenderer(@Nonnull JComponent actualComponent) {
        super(
            wrapWithLimitedWidth(actualComponent, CodeReviewChatItemUIUtil.TEXT_CONTENT_WIDTH + 52),
            ComponentInlayAlignment.FIT_VIEWPORT_WIDTH
        );
        this.actualComponent = actualComponent;
    }

    public boolean isVisible() {
        return actualComponent.isVisible();
    }

    public void setVisible(boolean value) {
        actualComponent.setVisible(value);
    }

    private static @Nonnull JComponent wrapWithLimitedWidth(@Nonnull JComponent component, int width) {
        JPanel panel = new JPanel(null);
        panel.setOpaque(false);
        DimensionRestrictions.ScalingConstant widthRestriction = new DimensionRestrictions.ScalingConstant(width, -1);
        SizeRestrictedSingleComponentLayout layout = new SizeRestrictedSingleComponentLayout();
        layout.setPrefSize(widthRestriction);
        layout.setMaxSize(widthRestriction);
        panel.setLayout(layout);
        panel.add(component);
        return panel;
    }
}
