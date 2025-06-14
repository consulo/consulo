// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.application.options.colors.highlighting;

import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.colorScheme.TextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import java.awt.*;

import static consulo.codeEditor.CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES;

public final class RendererWrapper implements EditorCustomElementRenderer {
    private final EditorCustomElementRenderer myDelegate;
    private boolean myDrawBorder;

    public RendererWrapper(EditorCustomElementRenderer delegate, boolean drawBorder) {
        myDelegate = delegate;
        myDrawBorder = drawBorder;
    }

    @Override
    public int calcWidthInPixels(@Nonnull Inlay inlay) {
        return myDelegate.calcWidthInPixels(inlay);
    }

    @Override
    public int calcHeightInPixels(@Nonnull Inlay inlay) {
        return myDelegate.calcHeightInPixels(inlay);
    }

    @Override
    public void paint(@Nonnull Inlay inlay, @Nonnull Graphics g, @Nonnull Rectangle r, @Nonnull TextAttributes textAttributes) {
        myDelegate.paint(inlay, g, r, textAttributes);
        if (myDrawBorder) {
            TextAttributes attributes = inlay.getEditor().getColorsScheme().getAttributes(BLINKING_HIGHLIGHTS_ATTRIBUTES);
            if (attributes != null && attributes.getEffectColor() != null) {
                g.setColor(TargetAWT.to(attributes.getEffectColor()));
                g.drawRect(r.x, r.y, r.width, r.height);
            }
        }
    }
}
