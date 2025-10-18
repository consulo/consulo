/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.ui;

import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.Gray;
import consulo.annotation.DeprecationInfo;

import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.Objects;

/**
 * @author Konstantin Bulenkov
 */
@Deprecated
@DeprecationInfo("Desktop only")
public class ColorIcon extends EmptyIcon {
    private final Color myColor;
    private boolean myBorder;
    private int myColorSize;

    public ColorIcon(int size, int colorSize, @Nonnull Color color, boolean border) {
        super(size, size);
        myColor = color;
        myColorSize = colorSize;
        myBorder = border;
    }

    public ColorIcon(int size, @Nonnull Color color, boolean border) {
        this(size, size, color, border);
    }

    public ColorIcon(int size, @Nonnull Color color) {
        this(size, color, false);
    }

    protected ColorIcon(ColorIcon icon) {
        super(icon);
        myColor = icon.myColor;
        myBorder = icon.myBorder;
        myColorSize = icon.myColorSize;
    }

    @Nonnull
    @Override
    protected ColorIcon copy() {
        return new ColorIcon(this);
    }

    public Color getIconColor() {
        return myColor;
    }

    @Override
    public void paintIcon(Component component, Graphics g, int i, int j) {
        int iconWidth = getIconWidth();
        int iconHeight = getIconHeight();
        g.setColor(getIconColor());

        int size = getColorSize();
        int x = i + (iconWidth - size) / 2;
        int y = j + (iconHeight - size) / 2;

        g.fillRect(x, y, size, size);

        if (myBorder) {
            g.setColor(Gray.x00.withAlpha(40));
            g.drawRect(x, y, size, size);
        }
    }

    private int getColorSize() {
        return (int) Math.ceil(scaleVal(myColorSize));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ColorIcon that = (ColorIcon) o;

        return myBorder == that.myBorder
            && myColorSize == that.myColorSize
            && Objects.equals(myColor, that.myColor);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (myColor != null ? myColor.hashCode() : 0);
        result = 31 * result + (myBorder ? 1 : 0);
        result = 31 * result + myColorSize;
        return result;
    }
}
