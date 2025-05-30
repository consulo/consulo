// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetrics;
import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetricsStorage;
import consulo.ide.impl.idea.ui.paint.EffectPainter;
import consulo.language.editor.inlay.BasePresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

public class TextInlayPresentation extends BasePresentation {
    private final InlayTextMetricsStorage metricsStorage;
    private final boolean isSmall;
    private String text;

    public TextInlayPresentation(InlayTextMetricsStorage metricsStorage, boolean isSmall, String text) {
        this.metricsStorage = metricsStorage;
        this.isSmall = isSmall;
        this.text = text;
    }

    @Override
    public int getWidth() {
        return getMetrics().getStringWidth(text);
    }

    @Override
    public int getHeight() {
        return getMetrics().getFontHeight();
    }

    private InlayTextMetrics getMetrics() {
        return metricsStorage.getFontMetrics(isSmall);
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        Object savedHint = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        try {
            ColorValue foreground = attributes.getForegroundColor();
            if (foreground != null) {
                InlayTextMetrics metrics = getMetrics();
                Font font = metrics.getFont();
                g.setFont(font);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false));
                g.setColor(TargetAWT.to(foreground));
                g.drawString(text, 0, metrics.getFontBaseline());
                ColorValue effectColor = attributes.getEffectColor();
                if (effectColor != null) {
                    g.setColor(TargetAWT.to(effectColor));
                    EffectType effectType = attributes.getEffectType();
                    if (effectType == EffectType.LINE_UNDERSCORE) {
                        EffectPainter.LINE_UNDERSCORE.paint(g, 0, metrics.getAscent(), getWidth(), metrics.getDescent(), font);
                    }
                    else if (effectType == EffectType.BOLD_LINE_UNDERSCORE) {
                        EffectPainter.BOLD_LINE_UNDERSCORE.paint(g, 0, metrics.getAscent(), getWidth(), metrics.getDescent(), font);
                    }
                    else if (effectType == EffectType.STRIKEOUT) {
                        EffectPainter.STRIKE_THROUGH.paint(g, 0, metrics.getFontBaseline(), getWidth(), getHeight(), font);
                    }
                    else if (effectType == EffectType.WAVE_UNDERSCORE) {
                        EffectPainter.WAVE_UNDERSCORE.paint(g, 0, metrics.getAscent(), getWidth(), metrics.getDescent(), font);
                    }
                    else if (effectType == EffectType.BOLD_DOTTED_LINE) {
                        EffectPainter.BOLD_DOTTED_UNDERSCORE.paint(g, 0, metrics.getAscent(), getWidth(), metrics.getDescent(), font);
                    }
                }
            }
        }
        finally {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedHint);
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
