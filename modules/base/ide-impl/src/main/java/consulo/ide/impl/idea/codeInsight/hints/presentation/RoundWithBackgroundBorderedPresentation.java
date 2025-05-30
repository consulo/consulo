// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

public class RoundWithBackgroundBorderedPresentation extends StaticDelegatePresentation {
    private final ColorValue borderColor;
    private final int borderWidth;

    public RoundWithBackgroundBorderedPresentation(RoundWithBackgroundPresentation presentation) {
        this(presentation, null, 1);
    }

    public RoundWithBackgroundBorderedPresentation(RoundWithBackgroundPresentation presentation,
                                                   ColorValue borderColor,
                                                   int borderWidth) {
        super(presentation);
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        RoundWithBackgroundPresentation roundWithBackground = (RoundWithBackgroundPresentation) presentation;
        roundWithBackground.paint(g, attributes);
        ColorValue effectiveBorderColor = (borderColor != null) ? borderColor : attributes.getEffectColor();
        if (effectiveBorderColor != null) {
            var config = GraphicsUtil.setupAAPainting(g);
            g.setColor(TargetAWT.to(effectiveBorderColor));
            g.setStroke(new BasicStroke((float) borderWidth));
            g.drawRoundRect(0, 0, getWidth(), getHeight(), roundWithBackground.getArcWidth(), roundWithBackground.getArcHeight());
            config.restore();
        }
    }
}
