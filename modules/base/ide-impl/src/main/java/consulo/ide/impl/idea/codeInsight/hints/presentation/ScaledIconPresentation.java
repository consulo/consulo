// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetrics;
import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetricsStorage;
import consulo.language.editor.inlay.BasePresentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import java.awt.*;

public class ScaledIconPresentation extends BasePresentation {
    protected final InlayTextMetricsStorage metricsStorage;
    protected final boolean isSmall;
    protected final Component component;
    protected Image icon;

    public ScaledIconPresentation(InlayTextMetricsStorage metricsStorage,
                                  boolean isSmall,
                                  Image icon,
                                  Component component) {
        this.metricsStorage = metricsStorage;
        this.isSmall = isSmall;
        this.icon = icon;
        this.component = component;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
        fireContentChanged(new Rectangle(icon.getWidth(), icon.getHeight()));
    }

    protected InlayTextMetrics getMetrics() {
        return metricsStorage.getFontMetrics(isSmall);
    }

    protected double getScaleFactor() {
        return (double) getMetrics().getFontHeight() / icon.getHeight();
    }

    @Override
    public int getWidth() {
        return (int) (icon.getWidth() * getScaleFactor());
    }

    @Override
    public int getHeight() {
        return getMetrics().getFontHeight();
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setComposite(AlphaComposite.SrcAtop.derive(1.0f));
        Image scaledImage = ImageEffects.resize(icon, (float) getScaleFactor());
        TargetAWT.to(scaledImage).paintIcon(component, graphics, 0, 0);
        graphics.dispose();
    }

    @Override
    public String toString() {
        return "<image>";
    }
}