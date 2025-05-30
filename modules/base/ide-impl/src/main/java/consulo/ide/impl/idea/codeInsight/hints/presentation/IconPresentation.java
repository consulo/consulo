// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.BasePresentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import java.awt.*;

public class IconPresentation extends BasePresentation {
    private final Component component;
    private Image icon;

    public IconPresentation(Image icon, Component component) {
        this.icon = icon;
        this.component = component;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
        fireContentChanged(new Rectangle(getWidth(), getHeight()));
    }

    @Override
    public int getWidth() {
        return icon.getWidth();
    }

    @Override
    public int getHeight() {
        return icon.getHeight();
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setComposite(AlphaComposite.SrcAtop.derive(1.0f));
        TargetAWT.to(icon).paintIcon(component, graphics, 0, 0);
        graphics.dispose();
    }

    @Override
    public String toString() {
        return "<image>";
    }
}
