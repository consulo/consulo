/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.avatar;

import consulo.ui.AvatarStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.impl.avatar.AvatarPresentation;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * The picture of a person: the image filling the circle when there is one, the initials on a colour of the name otherwise.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public class AvatarComponent extends JComponent {
    private String myName = "";
    private @Nullable Image myImage;
    private int mySize;
    private @Nullable AvatarStyle myOutline;

    public AvatarComponent(@Nullable AvatarStyle style) {
        mySize = AvatarPresentation.sizeOf(style);
    }

    public void setAvatarName(String name) {
        myName = name;
        setToolTipText(name.isEmpty() ? null : name);
        repaint();
    }

    public void setImage(@Nullable Image image) {
        myImage = image;
        repaint();
    }

    public void setStyle(@Nullable AvatarStyle style) {
        if (style != null && AvatarPresentation.isOutline(style)) {
            myOutline = style;
        }
        else {
            mySize = AvatarPresentation.sizeOf(style);
        }
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(JBUI.scale(mySize), JBUI.scale(mySize));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int size = JBUI.scale(mySize);
        ColorValue outlineColor = AvatarPresentation.outlineColorOf(myOutline);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float stroke = JBUI.scale(outlineColor == null ? 1f : (float) AvatarPresentation.OUTLINE_WIDTH);
            Ellipse2D circle = new Ellipse2D.Float(stroke / 2, stroke / 2, size - stroke, size - stroke);
            g2.setColor(TargetAWT.to(ComponentColors.COMPONENT_BACKGROUND));
            g2.fill(circle);

            Image image = AvatarPresentation.imageOf(myName, myImage);
            if (image != null) {
                Icon icon = TargetAWT.to(ImageEffects.resize(image, mySize, mySize));
                Graphics2D clipped = (Graphics2D) g2.create();
                try {
                    clipped.clip(new Ellipse2D.Float(0, 0, size, size));
                    icon.paintIcon(this, clipped, (size - icon.getIconWidth()) / 2, (size - icon.getIconHeight()) / 2);
                }
                finally {
                    clipped.dispose();
                }
            }
            else {
                String initials = AvatarPresentation.initialsOf(myName);
                g2.setFont(UIUtil.getLabelFont().deriveFont(Font.PLAIN, size * 0.42f));
                FontMetrics metrics = g2.getFontMetrics();
                g2.setColor(colorOf(myName));
                g2.drawString(initials, (size - metrics.stringWidth(initials)) / 2f, (size - metrics.getHeight()) / 2f + metrics.getAscent());
            }

            g2.setColor(TargetAWT.to(outlineColor == null ? ComponentColors.BORDER : outlineColor));
            g2.setStroke(new BasicStroke(stroke));
            g2.draw(circle);
        }
        finally {
            g2.dispose();
        }
    }

    static Color colorOf(String name) {
        boolean dark = StyleManager.get().getCurrentStyle().isDark();
        return Color.getHSBColor(
            AvatarPresentation.hueOf(name),
            AvatarPresentation.saturation(dark),
            AvatarPresentation.brightness(dark)
        );
    }
}
