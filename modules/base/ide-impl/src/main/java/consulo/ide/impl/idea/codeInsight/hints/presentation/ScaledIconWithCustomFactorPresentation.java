// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetricsStorage;
import consulo.ui.image.Image;

import java.awt.*;

public class ScaledIconWithCustomFactorPresentation extends ScaledIconPresentation {
    private final float iconScaleFactor;

    public ScaledIconWithCustomFactorPresentation(InlayTextMetricsStorage metricsStorage,
                                                  boolean isSmall,
                                                  Image icon,
                                                  Component component,
                                                  float iconScaleFactor) {
        super(metricsStorage, isSmall, icon, component);
        this.iconScaleFactor = iconScaleFactor;
    }

    @Override
    public int getHeight() {
        return (int) Math.ceil(super.getHeight() * iconScaleFactor);
    }

    @Override
    protected double getScaleFactor() {
        return super.getScaleFactor() * iconScaleFactor;
    }
}