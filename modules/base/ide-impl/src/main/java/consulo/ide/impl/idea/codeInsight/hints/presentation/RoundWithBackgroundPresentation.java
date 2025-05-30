// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

public class RoundWithBackgroundPresentation extends StaticDelegatePresentation {
    private final int arcWidth;
    private final int arcHeight;
    private final ColorValue color;
    private final float backgroundAlpha;

    public RoundWithBackgroundPresentation(InlayPresentation presentation,
                                           int arcWidth,
                                           int arcHeight) {
        this(presentation, arcWidth, arcHeight, null, 0.55f);
    }

    public RoundWithBackgroundPresentation(InlayPresentation presentation,
                                           int arcWidth,
                                           int arcHeight,
                                           ColorValue color,
                                           float backgroundAlpha) {
        super(presentation);
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.color = color;
        this.backgroundAlpha = backgroundAlpha;
    }

    public int getArcHeight() {
        return arcHeight;
    }

    public int getArcWidth() {
        return arcWidth;
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        ColorValue backgroundColor = (color != null) ? color : attributes.getBackgroundColor();
        if (backgroundColor != null) {
            float alpha = backgroundAlpha;
            var config = GraphicsUtil.setupAAPainting(g);
            GraphicsUtil.paintWithAlpha(g, alpha);
            g.setColor(TargetAWT.to(backgroundColor));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), arcWidth, arcHeight);
            config.restore();
        }
        presentation.paint(g, attributes);
    }
}
