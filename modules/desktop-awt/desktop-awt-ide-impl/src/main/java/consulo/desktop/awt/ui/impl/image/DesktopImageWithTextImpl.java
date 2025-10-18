/*
 * Copyright 2013-2020 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.ui.impl.image;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.JBFont;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Objects;

import static consulo.ui.ex.awt.JBUI.ScaleType.OBJ_SCALE;

/**
 * @author VISTALL
 * @since 2020-08-01
 */
public class DesktopImageWithTextImpl extends JBUI.ScalableJBIcon implements Icon, Image {
    @Nonnull
    private final String myText;
    private final WeakReference<Component> myCompRef;
    private final int myFontSize;

    private Font myFont;
    private FontMetrics myMetrics;

    public DesktopImageWithTextImpl(@Nonnull String text, @Nonnull Component component, int fontSize) {
        myText = text;
        myFontSize = fontSize;
        myCompRef = new WeakReference<>(component);
        setIconPreScaled(false);
        getScaleContext().addUpdateListener(this::update);
        update();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) { // x,y is in USR_SCALE
        g = g.create();
        try {
            GraphicsUtil.setupAntialiasing(g);
            g.setFont(myFont);
            UIUtil.drawStringWithHighlighting(
                g,
                myText,
                (int) scaleVal(x, OBJ_SCALE) + (int) scaleVal(2),
                (int) scaleVal(y, OBJ_SCALE) + getIconHeight() - (int) scaleVal(1),
                JBColor.foreground(),
                JBColor.background()
            );
        }
        finally {
            g.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return myMetrics.stringWidth(myText) + (int) scaleVal(4);
    }

    @Override
    public int getIconHeight() {
        return myMetrics.getHeight();
    }

    private void update() {
        myFont = JBFont.create(JBFont.label().deriveFont((float) scaleVal(myFontSize, OBJ_SCALE))); // fontSize is in USR_SCALE
        Component comp = myCompRef.get();
        if (comp == null) {
            comp = new Component() {
            };
        }
        myMetrics = comp.getFontMetrics(myFont);
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof DesktopImageWithTextImpl that
            && Objects.equals(myText, that.myText)
            && Objects.equals(myFont, that.myFont);
    }

    @Override
    public int getHeight() {
        return getIconHeight();
    }

    @Override
    public int getWidth() {
        return getIconWidth();
    }
}
