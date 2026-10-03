/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.desktop.awt.ui.impl.adtui.instructions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JComponent;

import org.jspecify.annotations.Nullable;

/**
 * Instruction for rendering icon.
 */
public final class IconInstruction extends RenderInstruction {
    private final Icon myIcon;
    private final int myPadding;
    @Nullable private final Color myBgColor;
    private final Dimension mySize;

    public IconInstruction(Icon icon, int padding, @Nullable Color bgColor) {
        myIcon = icon;
        myPadding = padding;
        myBgColor = bgColor;
        mySize = new Dimension(myIcon.getIconWidth() + 2 * myPadding, myIcon.getIconHeight() + 2 * myPadding);
    }

    @Override
    public Dimension getSize() {
        return mySize;
    }

    public Icon getIcon() {
        return myIcon;
    }

    @Override
    public void render(JComponent c, Graphics2D g2d, Rectangle bounds) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        assert (mySize.height <= bounds.height && mySize.width <= bounds.width);

        if (myBgColor != null) {
            g2d.setColor(myBgColor);
            g2d.fillRoundRect(bounds.x, bounds.y, mySize.width, mySize.height, 5, 5);
        }

        int iconY = bounds.y + (bounds.height - myIcon.getIconHeight()) / 2;
        myIcon.paintIcon(c, g2d, bounds.x + myPadding, iconY);
    }
}
